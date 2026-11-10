package com.example.domain.ai

import android.util.Log
import androidx.compose.runtime.Immutable
import com.example.data.local.TransactionType
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.ui.FinanceVaultUiState
import com.example.ui.MainTab
import com.example.ui.MoreDestination
import com.example.ui.ReportTab
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "GeminiVaultAssistant"

@Immutable
data class ParsedVoiceTransaction(
    val transactionType: TransactionType,
    val amountPaise: Long,
    val category: String,
    val description: String,
    val locationName: String = "",
    val sourceAccountHint: String = "",
    val destinationAccountHint: String = "",
    val spokenConfirmation: String = "",
    val isCategoryExplicit: Boolean = true
)

@Immutable
data class GeminiAssistantResult(
    val action: String,
    val transactionsToRecord: List<ParsedVoiceTransaction> = emptyList(),
    val targetTab: MainTab? = null,
    val targetMore: MoreDestination? = null,
    val targetReport: ReportTab? = null,
    val targetLedgerAccountHint: String = "",
    val entityName: String = "",
    val primaryAmountPaise: Long = 0L,
    val secondaryAmountPaise: Long = 0L,
    val ratePercent: Double = 8.5,
    val tenureOrIntervalCount: Int = 1,
    val frequencyOrTypeHint: String = "",
    val categoryOrScope: String = "",
    val accountHint: String = "",
    val searchQuery: String = "",
    val settingKey: String = "",
    val settingValue: String = "",
    val exportFormat: String = "",
    val spokenReply: String,
    val errorMessage: String? = null
)

object GeminiVaultAssistant {

    private val assistantResponseSchema = Schema.obj(
        properties = mapOf(
            "action" to Schema.enumeration(
                values = listOf(
                    "RECORD_TRANSACTION",
                    "DELETE_TRANSACTION",
                    "NAVIGATE",
                    "CREATE_ACCOUNT",
                    "ARCHIVE_ACCOUNT",
                    "ADD_SUBSCRIPTION",
                    "PAY_SUBSCRIPTION",
                    "DELETE_SUBSCRIPTION",
                    "ADD_INVESTMENT",
                    "ADD_LOAN",
                    "PAY_LOAN_EMI",
                    "ADD_BUDGET",
                    "DELETE_BUDGET",
                    "ADD_GOAL",
                    "DELETE_GOAL",
                    "CONTRIBUTE_GOAL",
                    "ADD_GIFT_CARD",
                    "ADD_TAX_RECORD",
                    "SEARCH",
                    "CLEAR_SEARCH",
                    "REVERSE_LAST_TRANSACTION",
                    "UNDO_LAST_ACTION",
                    "EXPORT_REPORT",
                    "UPDATE_SETTING",
                    "QUERY_INFO",
                    "NONE"
                ),
                description = "Primary full-app action requested by the user."
            ),
            "transactionType" to Schema.enumeration(
                values = listOf("EXPENSE", "INCOME", "TRANSFER"),
                description = "Transaction type if action is RECORD_TRANSACTION."
            ),
            "amountInRupees" to Schema.double(
                description = "Primary amount in INR Rupees (e.g. transaction amount, opening balance, subscription cost, investment amount, budget limit, goal target, loan principal). Set to 0 if not applicable."
            ),
            "secondaryAmountInRupees" to Schema.double(
                description = "Optional secondary amount in INR Rupees (e.g. initial saved amount on a goal or credit limit)."
            ),
            "ratePercent" to Schema.double(
                description = "Interest rate percentage for loans or investments (e.g. 8.5)."
            ),
            "tenureOrIntervalCount" to Schema.integer(
                description = "Loan tenure in months, investment duration in months, or custom subscription interval count (e.g. 2 for every 2 months)."
            ),
            "entityName" to Schema.string(
                description = "Name of account, subscription, investment, loan, budget, goal, gift card brand, or tax challan."
            ),
            "frequencyOrTypeHint" to Schema.string(
                description = "Account type (BANK, CASH, UPI_WALLET, CREDIT_CARD), subscription frequency (MONTHLY, BIMONTHLY, FOUR_MONTHLY, YEARLY, WEEKLY, CUSTOM), investment category (MUTUAL_FUND, EQUITY_SHARES, FIXED_DEPOSIT, PPF, GOLD, NPS), loan type (HOME, VEHICLE, PERSONAL, EDUCATION, CREDIT_CARD), or tax type."
            ),
            "category" to Schema.string(
                description = "Transaction or budget category such as Food & Dining, Household & Groceries, Transportation & Fuel, Utilities & Recharge, Shopping & Lifestyle, Healthcare & Medical, Entertainment & Subscriptions, Education, Salary Income, Business & Freelance, or Other."
            ),
            "description" to Schema.string(
                description = "Short clear narration or notes."
            ),
            "locationName" to Schema.string(
                description = "Merchant, store, or place name mentioned by the user (or empty string)."
            ),
            "sourceAccountHint" to Schema.string(
                description = "Source or payment bank/cash/wallet account name mentioned by the user (or empty string)."
            ),
            "destinationAccountHint" to Schema.string(
                description = "Destination account name if transfer or income (or empty string)."
            ),
            "navigationScreen" to Schema.string(
                description = "Screen to open: HOME, ACCOUNTS, ACTIVITY, INVESTMENTS, MORE_HUB, REPORTS, ANALYTICS, LOANS, BUDGETS_GOALS, GIFT_CARDS, SUBSCRIPTIONS, TAXES, SETTINGS, BALANCE_SHEET, PROFIT_AND_LOSS, CASH_FLOW, TRIAL_BALANCE, ACCOUNT_LEDGER, JOURNAL_BOOK, SCHEDULES, or NONE."
            ),
            "searchQuery" to Schema.string(
                description = "Search query string if action is SEARCH."
            ),
            "settingKey" to Schema.string(
                description = "Setting key if UPDATE_SETTING: PRIVACY_MODE, THEME_MODE, DEMO_MODE, LOCK_APP, or ADD_REMINDER."
            ),
            "settingValue" to Schema.string(
                description = "Setting value: ON, OFF, DARK, LIGHT, SYSTEM, or reminder time (e.g. 21:00)."
            ),
            "exportFormat" to Schema.string(
                description = "Export format if EXPORT_REPORT: PDF, EXCEL, CSV, or JSON."
            ),
            "spokenReply" to Schema.string(
                description = "Concise, natural English spoken response confirming the action performed or answering the user's question."
            )
        ),
        optionalProperties = listOf(
            "secondaryAmountInRupees",
            "ratePercent",
            "tenureOrIntervalCount",
            "entityName",
            "frequencyOrTypeHint",
            "locationName",
            "sourceAccountHint",
            "destinationAccountHint",
            "navigationScreen",
            "searchQuery",
            "settingKey",
            "settingValue",
            "exportFormat"
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = assistantResponseSchema
            }
        )
    }

    /**
     * Full-App Omni-Voice Command Interpreter.
     * Supports:
     * 1. Instant deterministic English parsing for navigation, undo, reversals, search, settings,
     *    exports, module entity creation, and transactions.
     * 2. Firebase AI Logic (`gemini-3.8-flash`) with full live Finance Vault state for complex,
     *    conversational, or multi-parameter commands.
     */
    suspend fun interpretVoiceOrGeminiCommand(
        utterance: String,
        uiState: FinanceVaultUiState
    ): GeminiAssistantResult {
        val cleaned = stripWakeWord(utterance).trim()
        if (cleaned.isBlank()) {
            return GeminiAssistantResult(
                action = "NONE",
                spokenReply = "I did not catch that. You can ask to open any screen, record a transaction, create an account, subscription, budget, or ask any financial question."
            )
        }

        // 1. Check fast deterministic local Omni-Command router first (Navigation, Undo, Search, Settings, Exports, Clear Creations)
        val localOmni = parseOmniCommandLocally(cleaned, uiState)
        if (localOmni != null) {
            return localOmni
        }

        val isQuestion = looksLikeFinancialQuery(cleaned)
        val localTx = parseEnglishTransactionLocally(cleaned, uiState)

        // 2. Fast path: Clear single transaction command with no extra question
        if (localTx != null && !isQuestion) {
            return GeminiAssistantResult(
                action = "RECORD_TRANSACTION",
                transactionsToRecord = listOf(localTx),
                spokenReply = localTx.spokenConfirmation
            )
        }

        // 3. Consult Gemini 3.8 Flash with full live Finance Vault context & Omni-Voice schema
        val vaultContextSummary = buildVaultContextPrompt(uiState)
        val prompt = """
            You are the built-in full-app voice controller and financial assistant for the Finance Vault Android app.
            The user spoke or sent the following English command:
            "$cleaned"

            Current live Finance Vault state:
            $vaultContextSummary

            Instructions:
            1. You can control the ENTIRE app by voice. Choose the best `action`:
               - `NAVIGATE`: User wants to open/view/see a screen, tab, module, or financial statement (e.g. Balance Sheet, Profit and Loss, Cash Flow, Trial Balance, General Ledger, Journal Book, Schedules, Accounts, Activity, Investments, Analytics, Loans, Budgets & Goals, Gift Cards, Subscriptions, Taxes, Settings, Home). Also set `navigationScreen` and give a helpful summary of that screen in `spokenReply`.
               - `RECORD_TRANSACTION`: User spent, paid, bought, received, earned, credited, or transferred money.
               - `CREATE_ACCOUNT` / `ARCHIVE_ACCOUNT`: User wants to create a new bank/cash/UPI/credit card account or archive one.
               - `ADD_SUBSCRIPTION` / `PAY_SUBSCRIPTION` / `DELETE_SUBSCRIPTION`: User wants to add, pay, or remove a recurring subscription.
               - `ADD_INVESTMENT`: User wants to record an investment (Mutual Fund, Stocks, FD, PPF, Gold, NPS).
               - `ADD_LOAN` / `PAY_LOAN_EMI`: User wants to add a loan or record an EMI payment.
               - `ADD_BUDGET` / `DELETE_BUDGET` / `ADD_GOAL` / `DELETE_GOAL` / `CONTRIBUTE_GOAL`: User wants to set or delete a budget, create or delete a savings goal, or add money to a goal.
               - `ADD_GIFT_CARD` / `ADD_TAX_RECORD`: User wants to add a gift card or tax payment record.
               - `SEARCH` / `CLEAR_SEARCH`: User wants to search/filter transactions or clear search.
               - `DELETE_TRANSACTION` / `REVERSE_LAST_TRANSACTION` / `UNDO_LAST_ACTION`: User wants to delete/reverse a transaction (by keyword, category, amount, or latest) or undo the last voice action.
               - `EXPORT_REPORT`: User wants to export/download a PDF, Excel, CSV, or JSON backup.
               - `UPDATE_SETTING`: User wants to toggle privacy mode (hide/show balances), change theme (dark/light/system), toggle demo mode, lock app, or add a reminder time.
               - `QUERY_INFO`: User is asking a question about their finances. Set `navigationScreen` to the most relevant screen so the app opens it automatically while speaking the answer in `spokenReply`.
            2. Keep `spokenReply` concise (1-2 sentences) and natural when read aloud in English.
        """.trimIndent()

        return try {
            val response = withTimeout(30_000L) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
            val json = JSONObject(jsonText)
            val action = json.optString("action", if (localTx != null) "RECORD_TRANSACTION" else "QUERY_INFO")
            val amountRupees = json.optDouble("amountInRupees", 0.0)
            val amountPaise = (amountRupees * 100.0).toLong()
            val secondaryRupees = json.optDouble("secondaryAmountInRupees", 0.0)
            val secondaryPaise = (secondaryRupees * 100.0).toLong()
            val ratePercent = json.optDouble("ratePercent", 8.5)
            val tenureCount = json.optInt("tenureOrIntervalCount", 1).coerceAtLeast(1)
            val entityName = json.optString("entityName", "").trim()
            val freqOrType = json.optString("frequencyOrTypeHint", "").trim()
            val txTypeRaw = json.optString("transactionType", "EXPENSE")
            val txType = when (txTypeRaw.uppercase(Locale.US)) {
                "INCOME" -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            }
            val rawCategory = json.optString("category", "").trim()
            val explicitCatFromText = inferExplicitCategoryFromEnglish(
                cleaned.lowercase(Locale.US),
                txType,
                uiState.customOptionsMap["TX_CATEGORY"].orEmpty()
            )
            val isCatExplicit = explicitCatFromText != null ||
                (rawCategory.isNotBlank() && !rawCategory.equals("Other", ignoreCase = true) && !rawCategory.equals("General", ignoreCase = true))
            val category = explicitCatFromText
                ?: rawCategory.takeIf { it.isNotBlank() && !it.equals("Other", ignoreCase = true) }
                ?: inferCategoryFromEnglish(cleaned.lowercase(Locale.US), txType, uiState.customOptionsMap["TX_CATEGORY"].orEmpty())
            val locationName = json.optString("locationName", "").ifBlank { extractLocationOrMerchant(cleaned) }
            val rawDesc = json.optString("description", "").trim()
            val description = if (rawDesc.isBlank() ||
                rawDesc.equals(cleaned, ignoreCase = true) ||
                rawDesc.startsWith("Voice ", ignoreCase = true)
            ) {
                buildCleanNarration(cleaned, category, locationName, txType)
            } else {
                rawDesc
            }
            val srcHint = json.optString("sourceAccountHint", "")
            val dstHint = json.optString("destinationAccountHint", "")
            val navScreen = json.optString("navigationScreen", "NONE")
            val searchQuery = json.optString("searchQuery", "")
            val settingKey = json.optString("settingKey", "")
            val settingValue = json.optString("settingValue", "")
            val exportFormat = json.optString("exportFormat", "")
            val (navTab, navMore, navReport) = resolveNavigationTargets(navScreen, cleaned)

            val reply = json.optString("spokenReply", "").ifBlank {
                localTx?.spokenConfirmation ?: answerFinancialQueryLocally(cleaned, uiState)
            }

            val parsedTxList = if (action == "RECORD_TRANSACTION") {
                listOf(
                    ParsedVoiceTransaction(
                        transactionType = txType,
                        amountPaise = if (amountPaise > 0L) amountPaise else (localTx?.amountPaise ?: 0L),
                        category = category,
                        description = description,
                        locationName = locationName,
                        sourceAccountHint = srcHint.ifBlank { localTx?.sourceAccountHint.orEmpty() },
                        destinationAccountHint = dstHint.ifBlank { localTx?.destinationAccountHint.orEmpty() },
                        spokenConfirmation = reply,
                        isCategoryExplicit = isCatExplicit
                    )
                )
            } else {
                emptyList()
            }

            GeminiAssistantResult(
                action = if (parsedTxList.isNotEmpty() && action == "NONE") "RECORD_TRANSACTION" else action,
                transactionsToRecord = parsedTxList,
                targetTab = navTab,
                targetMore = navMore,
                targetReport = navReport,
                targetLedgerAccountHint = srcHint.ifBlank { entityName },
                entityName = entityName,
                primaryAmountPaise = amountPaise,
                secondaryAmountPaise = secondaryPaise,
                ratePercent = ratePercent,
                tenureOrIntervalCount = tenureCount,
                frequencyOrTypeHint = freqOrType,
                categoryOrScope = category,
                accountHint = srcHint.ifBlank { dstHint },
                searchQuery = searchQuery,
                settingKey = settingKey,
                settingValue = settingValue,
                exportFormat = exportFormat,
                spokenReply = reply
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out after 60s", e)
            fallbackLocalResult(cleaned, localTx, uiState, "Gemini timed out; executed using on-device voice engine.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini inference failed", e)
            fallbackLocalResult(cleaned, localTx, uiState, null)
        }
    }

    private fun fallbackLocalResult(
        cleaned: String,
        localTx: ParsedVoiceTransaction?,
        uiState: FinanceVaultUiState,
        errMessage: String?
    ): GeminiAssistantResult {
        if (localTx != null) {
            return GeminiAssistantResult(
                action = "RECORD_TRANSACTION",
                transactionsToRecord = listOf(localTx),
                spokenReply = localTx.spokenConfirmation,
                errorMessage = errMessage
            )
        }
        val (navTab, navMore, navReport) = resolveNavigationTargets("NONE", cleaned)
        val localAnswer = answerFinancialQueryLocally(cleaned, uiState)
        return GeminiAssistantResult(
            action = if (navTab != null || navMore != null || navReport != null) "NAVIGATE" else "QUERY_INFO",
            targetTab = navTab,
            targetMore = navMore,
            targetReport = navReport,
            spokenReply = localAnswer,
            errorMessage = errMessage
        )
    }

    /**
     * Instant deterministic English Omni-Command parser for zero-latency full-app voice control.
     */
    fun parseOmniCommandLocally(
        cleaned: String,
        uiState: FinanceVaultUiState
    ): GeminiAssistantResult? {
        val lower = cleaned.lowercase(Locale.US).trim()

        // 1. Voice Undo / Reverse Last Action
        if (
            lower == "undo" ||
            lower == "undo that" ||
            lower.contains("undo last") ||
            lower.contains("revert last")
        ) {
            return GeminiAssistantResult(
                action = "UNDO_LAST_ACTION",
                spokenReply = "Undoing your last action."
            )
        }

        // 1b. Comprehensive Instant Voice Delete / Remove / Reverse Router
        val hasDeleteVerb = Regex("""\b(?:delete|remove|reverse|erase|cancel)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(lower) &&
            !lower.contains("clear search") &&
            !lower.contains("close search")

        if (hasDeleteVerb) {
            val mentionedAmountPaise = extractAmountPaiseFromEnglish(cleaned) ?: 0L

            // Delete / Remove Subscription
            val matchedSubByName = uiState.subscriptions.firstOrNull {
                it.name.isNotBlank() && lower.contains(it.name.lowercase(Locale.US))
            }
            if (lower.contains("subscription") || (matchedSubByName != null && !lower.contains("transaction") && !lower.contains("expense"))) {
                val subName = matchedSubByName?.name
                    ?: extractDeleteTargetKeyword(cleaned, listOf("subscription", "subscriptions", "recurring"))
                return GeminiAssistantResult(
                    action = "DELETE_SUBSCRIPTION",
                    entityName = subName,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.SUBSCRIPTIONS,
                    spokenReply = if (subName.isNotBlank()) "Deleting $subName subscription." else "Checking subscriptions to delete."
                )
            }

            // Delete / Remove Budget
            if (lower.contains("budget")) {
                val budgetHint = extractDeleteTargetKeyword(cleaned, listOf("budget", "budgets", "monthly"))
                return GeminiAssistantResult(
                    action = "DELETE_BUDGET",
                    entityName = budgetHint,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.BUDGETS_GOALS,
                    spokenReply = if (budgetHint.isNotBlank()) "Deleting $budgetHint budget." else "Checking budgets to delete."
                )
            }

            // Delete / Remove Savings Goal
            if (lower.contains("goal")) {
                val goalHint = extractDeleteTargetKeyword(cleaned, listOf("savings", "goal", "goals"))
                return GeminiAssistantResult(
                    action = "DELETE_GOAL",
                    entityName = goalHint,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.BUDGETS_GOALS,
                    spokenReply = if (goalHint.isNotBlank()) "Deleting $goalHint goal." else "Checking savings goals to delete."
                )
            }

            // Delete / Archive Account
            if (lower.contains("account") || lower.contains("wallet")) {
                val accHint = extractDeleteTargetKeyword(cleaned, listOf("bank", "account", "accounts", "wallet"))
                return GeminiAssistantResult(
                    action = "ARCHIVE_ACCOUNT",
                    entityName = accHint,
                    targetTab = MainTab.ACCOUNTS,
                    spokenReply = if (accHint.isNotBlank()) "Archiving $accHint account." else "Checking accounts to archive."
                )
            }

            // Default Delete -> Delete / Reverse Transaction (by keyword, category, merchant, amount, or latest)
            val isExplicitLatest = lower.contains("last") || lower.contains("latest") || lower.contains("recent")
            val txKeyword = extractDeleteTargetKeyword(
                cleaned,
                listOf("transaction", "transactions", "expense", "expenses", "income", "entry", "record", "payment", "last", "latest", "recent", "something", "item")
            )
            return GeminiAssistantResult(
                action = "DELETE_TRANSACTION",
                entityName = txKeyword,
                primaryAmountPaise = mentionedAmountPaise,
                frequencyOrTypeHint = if (isExplicitLatest) "LATEST" else "",
                targetTab = MainTab.ACTIVITY,
                spokenReply = "Deleting matching transaction."
            )
        }

        // 2. Search & Filter Commands
        if (
            lower == "clear search" ||
            lower == "reset search" ||
            lower == "close search" ||
            lower == "clear filter"
        ) {
            return GeminiAssistantResult(
                action = "CLEAR_SEARCH",
                spokenReply = "Cleared active search filter."
            )
        }

        val searchRegex = Regex("""^(?:search\s+for|search|find\s+transactions?\s+for|find|filter\s+by)\s+(.+)$""", RegexOption.IGNORE_CASE)
        val searchMatch = searchRegex.find(cleaned)
        if (searchMatch != null) {
            val q = searchMatch.groupValues[1].trim()
            if (q.isNotBlank()) {
                val matchCount = uiState.transactions.count {
                    !it.isReversed && (
                        it.description.contains(q, ignoreCase = true) ||
                            it.category.contains(q, ignoreCase = true) ||
                            it.locationName.contains(q, ignoreCase = true)
                        )
                }
                return GeminiAssistantResult(
                    action = "SEARCH",
                    searchQuery = q,
                    targetTab = MainTab.ACTIVITY,
                    spokenReply = "Showing $matchCount matching transactions for $q."
                )
            }
        }

        // 3. Export & Backup Commands
        if (lower.contains("export") || lower.contains("download") || lower.contains("backup")) {
            when {
                lower.contains("pdf") -> return GeminiAssistantResult(
                    action = "EXPORT_REPORT",
                    exportFormat = "PDF",
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.REPORTS,
                    spokenReply = "Exporting your financial statements as a PDF document."
                )
                lower.contains("excel") || lower.contains("xlsx") || lower.contains("spreadsheet") -> return GeminiAssistantResult(
                    action = "EXPORT_REPORT",
                    exportFormat = "EXCEL",
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.REPORTS,
                    spokenReply = "Exporting your financial statements as an Excel spreadsheet."
                )
                lower.contains("csv") -> return GeminiAssistantResult(
                    action = "EXPORT_REPORT",
                    exportFormat = "CSV",
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.REPORTS,
                    spokenReply = "Exporting your financial statements as a CSV file."
                )
                lower.contains("json") || lower.contains("backup") -> return GeminiAssistantResult(
                    action = "EXPORT_REPORT",
                    exportFormat = "JSON",
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.SETTINGS,
                    spokenReply = "Creating a full JSON backup of your Finance Vault database."
                )
            }
        }

        // 4. App Settings Commands (Privacy Mode, Theme Mode, Demo Mode, Lock App)
        if (
            lower.contains("privacy mode") ||
            lower.contains("hide balance") ||
            lower.contains("hide my balance") ||
            lower.contains("show balance") ||
            lower.contains("show my balance") ||
            lower.contains("unhide balance")
        ) {
            val turnOn = when {
                lower.contains("off") || lower.contains("disable") || lower.contains("show") || lower.contains("unhide") -> "OFF"
                lower.contains("on") || lower.contains("enable") || lower.contains("hide") -> "ON"
                else -> if (uiState.settings.isPrivacyModeEnabled) "OFF" else "ON"
            }
            val msg = if (turnOn == "ON") "Privacy mode enabled. Your balances are now hidden." else "Privacy mode disabled. Your balances are now visible."
            return GeminiAssistantResult(
                action = "UPDATE_SETTING",
                settingKey = "PRIVACY_MODE",
                settingValue = turnOn,
                spokenReply = msg
            )
        }

        if (lower.contains("dark mode") || lower.contains("dark theme")) {
            return GeminiAssistantResult(
                action = "UPDATE_SETTING",
                settingKey = "THEME_MODE",
                settingValue = "DARK",
                spokenReply = "Switched appearance to Dark Mode."
            )
        }
        if (lower.contains("light mode") || lower.contains("light theme")) {
            return GeminiAssistantResult(
                action = "UPDATE_SETTING",
                settingKey = "THEME_MODE",
                settingValue = "LIGHT",
                spokenReply = "Switched appearance to Light Mode."
            )
        }
        if (lower.contains("system theme") || lower.contains("system mode")) {
            return GeminiAssistantResult(
                action = "UPDATE_SETTING",
                settingKey = "THEME_MODE",
                settingValue = "SYSTEM",
                spokenReply = "Switched appearance to follow system theme."
            )
        }
        if (lower.contains("demo mode") || lower.contains("demonstration mode") || lower.contains("real mode") || lower.contains("real records")) {
            val enableDemo = !lower.contains("off") && !lower.contains("disable") && !lower.contains("real")
            return GeminiAssistantResult(
                action = "UPDATE_SETTING",
                settingKey = "DEMO_MODE",
                settingValue = if (enableDemo) "ON" else "OFF",
                spokenReply = if (enableDemo) "Switched to demonstration mode." else "Switched to your real financial records."
            )
        }
        if (lower == "lock app" || lower == "lock vault" || lower == "lock finance vault") {
            return GeminiAssistantResult(
                action = "UPDATE_SETTING",
                settingKey = "LOCK_APP",
                settingValue = "ON",
                spokenReply = "Locking Finance Vault."
            )
        }

        // 5. Voice Entity Creation Commands across Modules
        val amountPaise = extractAmountPaiseFromEnglish(cleaned) ?: 0L

        // 5a. Create Account by Voice (e.g. "Create a bank account named SBI Savings with 25000 rupees")
        if (
            (lower.contains("create") || lower.contains("add") || lower.contains("open new")) &&
            (lower.contains("account") || lower.contains("wallet"))
        ) {
            val accType = when {
                lower.contains("cash") -> "CASH"
                lower.contains("upi") || lower.contains("wallet") -> "UPI_WALLET"
                lower.contains("credit card") -> "CREDIT_CARD"
                lower.contains("demat") || lower.contains("brokerage") -> "DEMAT"
                else -> "BANK"
            }
            val nameRegex = Regex("""(?:named|called|for|account|wallet)\s+([A-Za-z0-9][A-Za-z0-9 &.-]{1,28}?)(?=\s+(?:with|having|balance|opening|of|for|\d|₹|rs)|$)""", RegexOption.IGNORE_CASE)
            val extractedName = nameRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim()
                ?.replace(Regex("""^(?:a|an|the|bank|cash|upi|credit card)\s+""", RegexOption.IGNORE_CASE), "")
                ?.trim()
                .orEmpty()
                .ifBlank {
                    when (accType) {
                        "CASH" -> "Cash Wallet"
                        "UPI_WALLET" -> "UPI Wallet"
                        "CREDIT_CARD" -> "Credit Card"
                        else -> "Bank Account"
                    }
                }
            return GeminiAssistantResult(
                action = "CREATE_ACCOUNT",
                entityName = extractedName,
                primaryAmountPaise = amountPaise,
                frequencyOrTypeHint = accType,
                targetTab = MainTab.ACCOUNTS,
                spokenReply = "Created $extractedName account" +
                    if (amountPaise > 0L) " with opening balance of ${InrMoney.formatPaise(amountPaise)}." else "."
            )
        }

        // 5b. Add / Pay / Delete Subscription by Voice
        if (lower.contains("subscription")) {
            if (lower.contains("delete") || lower.contains("remove") || lower.contains("cancel")) {
                val subName = extractEntityNameAfterKeyword(cleaned, listOf("delete", "remove", "cancel"), "subscription")
                return GeminiAssistantResult(
                    action = "DELETE_SUBSCRIPTION",
                    entityName = subName,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.SUBSCRIPTIONS,
                    spokenReply = if (subName.isNotBlank()) "Removed $subName subscription." else "Opening Subscriptions."
                )
            }
            if (lower.contains("pay ") || lower.contains("renew ") || lower.contains("record payment")) {
                val subName = extractEntityNameAfterKeyword(cleaned, listOf("pay", "renew"), "subscription")
                return GeminiAssistantResult(
                    action = "PAY_SUBSCRIPTION",
                    entityName = subName,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.SUBSCRIPTIONS,
                    spokenReply = if (subName.isNotBlank()) "Recording payment for $subName subscription." else "Checking subscription to pay."
                )
            }
            if ((lower.contains("add") || lower.contains("create") || lower.contains("new")) && amountPaise > 0L) {
                val freq = when {
                    lower.contains("two month") || lower.contains("2 month") || lower.contains("bimonthly") -> "BIMONTHLY"
                    lower.contains("four month") || lower.contains("4 month") -> "FOUR_MONTHLY"
                    lower.contains("quarter") || lower.contains("3 month") -> "QUARTERLY"
                    lower.contains("half year") || lower.contains("6 month") -> "HALF_YEARLY"
                    lower.contains("year") || lower.contains("annual") -> "YEARLY"
                    lower.contains("week") -> "WEEKLY"
                    else -> "MONTHLY"
                }
                val subName = extractEntityNameAfterKeyword(cleaned, listOf("add", "create", "new"), "subscription")
                    .ifBlank { "Subscription" }
                return GeminiAssistantResult(
                    action = "ADD_SUBSCRIPTION",
                    entityName = subName,
                    primaryAmountPaise = amountPaise,
                    frequencyOrTypeHint = freq,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.SUBSCRIPTIONS,
                    spokenReply = "Added $subName subscription for ${InrMoney.formatPaise(amountPaise)} ($freq)."
                )
            }
        }

        // 5c. Add Investment by Voice (e.g. "Invest 15000 in Nifty 50 Index Fund" or "Add investment of 10000 in Gold")
        if (
            (lower.startsWith("invest ") || ((lower.contains("add") || lower.contains("record")) && lower.contains("investment"))) &&
            amountPaise > 0L
        ) {
            val invCat = when {
                lower.contains("stock") || lower.contains("equity") || lower.contains("share") -> "EQUITY_SHARES"
                lower.contains("fd") || lower.contains("fixed deposit") -> "FIXED_DEPOSIT"
                lower.contains("rd") || lower.contains("recurring deposit") -> "RECURRING_DEPOSIT"
                lower.contains("ppf") -> "PPF"
                lower.contains("gold") || lower.contains("silver") -> "GOLD"
                lower.contains("nps") || lower.contains("pension") -> "NPS"
                lower.contains("real estate") || lower.contains("property") -> "REAL_ESTATE"
                lower.contains("bond") || lower.contains("debenture") -> "BONDS"
                else -> "MUTUAL_FUND"
            }
            val inRegex = Regex("""\b(?:in|into|for)\s+([A-Za-z0-9][A-Za-z0-9 &.-]{1,30}?)(?=\s+(?:from|using|with|at|for|\d)|$)""", RegexOption.IGNORE_CASE)
            val invName = inRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { "Portfolio Investment" }
            return GeminiAssistantResult(
                action = "ADD_INVESTMENT",
                entityName = invName,
                primaryAmountPaise = amountPaise,
                frequencyOrTypeHint = invCat,
                targetTab = MainTab.INVESTMENTS,
                spokenReply = "Recorded investment of ${InrMoney.formatPaise(amountPaise)} in $invName."
            )
        }

        // 5d. Add Budget or Savings Goal by Voice
        if (lower.contains("budget") && (lower.contains("set") || lower.contains("add") || lower.contains("create")) && amountPaise > 0L) {
            val isOverall = lower.contains("overall") || lower.contains("total")
            val cat = if (isOverall) "OVERALL" else inferCategoryFromEnglish(lower, TransactionType.EXPENSE, uiState.customOptionsMap["TX_CATEGORY"].orEmpty())
            return GeminiAssistantResult(
                action = "ADD_BUDGET",
                entityName = if (isOverall) "Overall Monthly Budget" else "$cat Budget",
                primaryAmountPaise = amountPaise,
                categoryOrScope = cat,
                targetTab = MainTab.MORE,
                targetMore = MoreDestination.BUDGETS_GOALS,
                spokenReply = "Set ${if (isOverall) "overall monthly" else cat} budget to ${InrMoney.formatPaise(amountPaise)}."
            )
        }

        if (lower.contains("goal") && amountPaise > 0L) {
            if (lower.contains("contribute") || (lower.contains("add") && lower.contains("to ") && !lower.contains("create") && !lower.contains("new"))) {
                val goalName = extractEntityNameAfterKeyword(cleaned, listOf("to", "towards", "into"), "goal")
                return GeminiAssistantResult(
                    action = "CONTRIBUTE_GOAL",
                    entityName = goalName,
                    primaryAmountPaise = amountPaise,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.BUDGETS_GOALS,
                    spokenReply = "Added ${InrMoney.formatPaise(amountPaise)} towards your ${goalName.ifBlank { "savings" }} goal."
                )
            }
            if (lower.contains("create") || lower.contains("add") || lower.contains("set") || lower.contains("new")) {
                val forRegex = Regex("""\b(?:for|named|called)\s+([A-Za-z0-9][A-Za-z0-9 &.-]{1,26}?)(?=\s+(?:of|with|target|\d|₹|rs)|$)""", RegexOption.IGNORE_CASE)
                val goalName = forRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { "Savings Goal" }
                return GeminiAssistantResult(
                    action = "ADD_GOAL",
                    entityName = goalName,
                    primaryAmountPaise = amountPaise,
                    targetTab = MainTab.MORE,
                    targetMore = MoreDestination.BUDGETS_GOALS,
                    spokenReply = "Created savings goal $goalName with target ${InrMoney.formatPaise(amountPaise)}."
                )
            }
        }

        // 5e. Add Loan or Gift Card by Voice
        if (lower.contains("gift card") && (lower.contains("add") || lower.contains("create")) && amountPaise > 0L) {
            val brandRegex = Regex("""(?:add|create)\s+(?:a|an)?\s*([A-Za-z0-9 &.-]{2,20}?)\s+gift\s+card""", RegexOption.IGNORE_CASE)
            val brand = brandRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { "Gift Card" }
            return GeminiAssistantResult(
                action = "ADD_GIFT_CARD",
                entityName = brand,
                primaryAmountPaise = amountPaise,
                targetTab = MainTab.MORE,
                targetMore = MoreDestination.GIFT_CARDS,
                spokenReply = "Added $brand gift card worth ${InrMoney.formatPaise(amountPaise)}."
            )
        }

        if (lower.contains("loan") && (lower.contains("add") || lower.contains("create")) && amountPaise > 0L) {
            val loanType = when {
                lower.contains("home") || lower.contains("housing") -> "HOME"
                lower.contains("car") || lower.contains("vehicle") || lower.contains("auto") || lower.contains("bike") -> "VEHICLE"
                lower.contains("education") || lower.contains("student") -> "EDUCATION"
                lower.contains("gold") -> "GOLD"
                else -> "PERSONAL"
            }
            val rateMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:%|percent)""", RegexOption.IGNORE_CASE).find(cleaned)
            val rate = rateMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 9.5
            val monthsMatch = Regex("""(\d+)\s*months?""", RegexOption.IGNORE_CASE).find(cleaned)
            val yearsMatch = Regex("""(\d+)\s*years?""", RegexOption.IGNORE_CASE).find(cleaned)
            val tenure = monthsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: (yearsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.times(12))
                ?: 36
            return GeminiAssistantResult(
                action = "ADD_LOAN",
                entityName = "${loanType.lowercase(Locale.US).replaceFirstChar { it.uppercase() }} Loan",
                primaryAmountPaise = amountPaise,
                ratePercent = rate,
                tenureOrIntervalCount = tenure,
                frequencyOrTypeHint = loanType,
                targetTab = MainTab.MORE,
                targetMore = MoreDestination.LOANS,
                spokenReply = "Added $loanType loan of ${InrMoney.formatPaise(amountPaise)} at $rate% for $tenure months."
            )
        }

        // 6. Explicit Navigation & "Show me / Open / Go to" Commands across All Screens & Statements
        val isNavigationVerb = lower.startsWith("open ") ||
            lower.startsWith("go to ") ||
            lower.startsWith("navigate to ") ||
            lower.startsWith("show ") ||
            lower.startsWith("show me ") ||
            lower.startsWith("view ") ||
            lower.startsWith("take me to ") ||
            lower.startsWith("switch to ")

        val (navTab, navMore, navReport) = resolveNavigationTargets("NONE", cleaned)
        if ((isNavigationVerb || lower.endsWith("screen") || lower.endsWith("tab") || lower.endsWith("statement") || lower.endsWith("report")) &&
            (navTab != null || navMore != null || navReport != null)
        ) {
            val ledgerHint = if (navReport == ReportTab.ACCOUNT_LEDGER) {
                extractAccountHintForLedger(cleaned, uiState)
            } else ""
            val summaryReply = buildNavigationSpokenSummary(navTab, navMore, navReport, ledgerHint, uiState)
            return GeminiAssistantResult(
                action = "NAVIGATE",
                targetTab = navTab,
                targetMore = navMore,
                targetReport = navReport,
                targetLedgerAccountHint = ledgerHint,
                spokenReply = summaryReply
            )
        }

        return null
    }

    fun resolveNavigationTargets(
        schemaScreen: String,
        utterance: String
    ): Triple<MainTab?, MoreDestination?, ReportTab?> {
        val code = schemaScreen.uppercase(Locale.US)
        val lower = utterance.lowercase(Locale.US)

        return when {
            code == "BALANCE_SHEET" || lower.contains("balance sheet") || lower.contains("net worth") || lower.contains("assets and liabilities") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.BALANCE_SHEET)
            code == "PROFIT_AND_LOSS" || lower.contains("profit and loss") || lower.contains("p&l") || lower.contains("p & l") || lower.contains("income statement") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.PROFIT_AND_LOSS)
            code == "CASH_FLOW" || lower.contains("cash flow") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.CASH_FLOW)
            code == "TRIAL_BALANCE" || lower.contains("trial balance") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.TRIAL_BALANCE)
            code == "ACCOUNT_LEDGER" || lower.contains("general ledger") || lower.contains("account ledger") || lower.contains("ledger") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.ACCOUNT_LEDGER)
            code == "JOURNAL_BOOK" || lower.contains("journal book") || lower.contains("journal entries") || lower.contains("vouchers") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.JOURNAL_BOOK)
            code == "SCHEDULES" || lower.contains("schedules to accounts") || lower.contains("accounting schedules") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.SCHEDULES)
            code == "REPORTS" || lower.contains("financial statement") || lower.contains("statements") || lower.contains("reports") ->
                Triple(MainTab.MORE, MoreDestination.REPORTS, ReportTab.BALANCE_SHEET)
            code == "ANALYTICS" || lower.contains("analytics") || lower.contains("charts") || lower.contains("performance") || lower.contains("spending analysis") ->
                Triple(MainTab.MORE, MoreDestination.ANALYTICS, null)
            code == "LOANS" || lower.contains("loan") || lower.contains("emi") || lower.contains("credit card") ->
                Triple(MainTab.MORE, MoreDestination.LOANS, null)
            code == "BUDGETS_GOALS" || lower.contains("budget") || lower.contains("goal") ->
                Triple(MainTab.MORE, MoreDestination.BUDGETS_GOALS, null)
            code == "GIFT_CARDS" || lower.contains("gift card") || lower.contains("voucher") ->
                Triple(MainTab.MORE, MoreDestination.GIFT_CARDS, null)
            code == "SUBSCRIPTIONS" || lower.contains("subscription") || lower.contains("renewal") ->
                Triple(MainTab.MORE, MoreDestination.SUBSCRIPTIONS, null)
            code == "TAXES" || lower.contains("tax") || lower.contains("tds") || lower.contains("challan") ->
                Triple(MainTab.MORE, MoreDestination.TAXES, null)
            code == "SETTINGS" || lower.contains("setting") || lower.contains("backup") || lower.contains("reminder") || lower.contains("about") ->
                Triple(MainTab.MORE, MoreDestination.SETTINGS, null)
            code == "MORE_HUB" || lower == "more" || lower == "open more" || lower == "more menu" ->
                Triple(MainTab.MORE, MoreDestination.HUB, null)
            code == "INVESTMENTS" || lower.contains("investment") || lower.contains("portfolio") || lower.contains("mutual fund") ->
                Triple(MainTab.INVESTMENTS, null, null)
            code == "ACCOUNTS" || lower.contains("accounts") || lower.contains("bank account") || lower.contains("balances") ->
                Triple(MainTab.ACCOUNTS, null, null)
            code == "ACTIVITY" || lower.contains("activity") || lower.contains("transaction history") || lower.contains("recent transactions") || lower.contains("all transactions") ->
                Triple(MainTab.ACTIVITY, null, null)
            code == "HOME" || lower.contains("home") || lower.contains("dashboard") || lower.contains("main screen") ->
                Triple(MainTab.HOME, null, null)
            else -> Triple(null, null, null)
        }
    }

    private fun buildNavigationSpokenSummary(
        tab: MainTab?,
        more: MoreDestination?,
        report: ReportTab?,
        ledgerHint: String,
        uiState: FinanceVaultUiState
    ): String {
        val totalBalance = uiState.accountsWithBalances
            .filter { !it.account.isArchived }
            .sumOf { it.closingBalancePaise }
        return when {
            report == ReportTab.BALANCE_SHEET ->
                "Opened Balance Sheet. Your total assets are ${InrMoney.formatPaise(uiState.balanceSheet.totalAssetsPaise)}, liabilities are ${InrMoney.formatPaise(uiState.balanceSheet.totalLiabilitiesPaise)}, and net worth is ${InrMoney.formatPaise(uiState.balanceSheet.totalNetWorthPaise)}."
            report == ReportTab.PROFIT_AND_LOSS ->
                "Opened Profit and Loss Account. Your financial year income is ${InrMoney.formatPaise(uiState.fyProfitAndLoss.totalIncomePaise)} and total expenses are ${InrMoney.formatPaise(uiState.fyProfitAndLoss.totalExpensesPaise)}."
            report == ReportTab.CASH_FLOW ->
                "Opened Cash Flow Statement. Net operating cash flow is ${InrMoney.formatPaise(uiState.fyCashFlow.netCashFromOperatingPaise)}."
            report == ReportTab.TRIAL_BALANCE ->
                "Opened Trial Balance with ${uiState.trialBalance.rows.size} ledger accounts totaling ${InrMoney.formatPaise(uiState.trialBalance.totalDebitPaise)}."
            report == ReportTab.ACCOUNT_LEDGER ->
                if (ledgerHint.isNotBlank()) "Opened General Ledger for $ledgerHint." else "Opened General Ledger."
            report == ReportTab.JOURNAL_BOOK ->
                "Opened Journal Book showing ${uiState.journalBook.entriesCount} double-entry vouchers."
            report == ReportTab.SCHEDULES ->
                "Opened Schedules to Accounts."
            more == MoreDestination.ANALYTICS ->
                "Opened Analytics and Performance. This month you spent ${InrMoney.formatPaise(uiState.currentMonthTotalSpentPaise)}."
            more == MoreDestination.SUBSCRIPTIONS ->
                "Opened Subscriptions. You have ${uiState.subscriptions.size} active subscriptions."
            more == MoreDestination.BUDGETS_GOALS ->
                "Opened Budgets and Savings Goals. You have ${uiState.budgets.size} budgets and ${uiState.goals.size} goals."
            more == MoreDestination.LOANS ->
                "Opened Loans and Credit Cards. You have ${uiState.loans.size} active loan records."
            more == MoreDestination.GIFT_CARDS ->
                "Opened Gift Cards. You have ${uiState.giftCards.size} gift cards."
            more == MoreDestination.TAXES ->
                "Opened Indian Tax Records for ${uiState.settings.activeFinancialYear}."
            more == MoreDestination.SETTINGS ->
                "Opened Settings, Reminders, and Backup."
            tab == MainTab.ACCOUNTS ->
                "Opened Accounts. Across ${uiState.accountsWithBalances.count { !it.account.isArchived }} active accounts, your total balance is ${InrMoney.formatPaise(totalBalance)}."
            tab == MainTab.ACTIVITY ->
                "Opened Activity showing ${uiState.transactions.count { !it.isReversed }} recorded transactions."
            tab == MainTab.INVESTMENTS -> {
                val totalInv = uiState.investments.sumOf { it.currentRecordedValuePaise ?: it.investedAmountPaise }
                "Opened Investments. Your portfolio has ${uiState.investments.size} holdings valued at ${InrMoney.formatPaise(totalInv)}."
            }
            tab == MainTab.HOME ->
                "Opened Home Dashboard. Your net worth is ${InrMoney.formatPaise(uiState.balanceSheet.totalNetWorthPaise)}."
            else -> "Opened requested screen."
        }
    }

    private fun extractAccountHintForLedger(text: String, uiState: FinanceVaultUiState): String {
        val lower = text.lowercase(Locale.US)
        for (acc in uiState.accountsWithBalances.map { it.account }) {
            if (acc.name.isNotBlank() && lower.contains(acc.name.lowercase(Locale.US))) {
                return acc.name
            }
        }
        return ""
    }

    private fun extractEntityNameAfterKeyword(
        text: String,
        verbs: List<String>,
        trailingTypeWord: String
    ): String {
        val verbPattern = verbs.joinToString("|")
        val regex = Regex("""\b(?:$verbPattern)\s+(?:my\s+|the\s+|a\s+|an\s+)?([A-Za-z0-9][A-Za-z0-9 &.-]{1,26}?)(?=\s+$trailingTypeWord|\s+for|\s+of|\s+every|\s+at|\s+\d|$)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ?.replace(Regex("""\b$trailingTypeWord\b""", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()
    }

    private fun extractDeleteTargetKeyword(
        text: String,
        stopWords: List<String>
    ): String {
        val allStopWords = (listOf(
            "please", "can", "you", "could", "would", "want", "to", "delete", "remove", "reverse",
            "erase", "cancel", "archive", "my", "the", "a", "an", "for", "of", "from", "in", "on",
            "at", "rupees", "rupee", "rs", "inr", "that", "this", "it"
        ) + stopWords).map { it.lowercase(Locale.US) }.toSet()

        return text
            .replace(Regex("""(?:₹|rs\.?|inr)?\s*\d[\d,]*(?:\.\d{1,2})?\s*(?:k|thousand|lakh|lakhs|lac|crore|crores)?""", RegexOption.IGNORE_CASE), " ")
            .split(Regex("""[^A-Za-z0-9&]+"""))
            .map { it.trim() }
            .filter { token -> token.length >= 2 && token.lowercase(Locale.US) !in allStopWords }
            .joinToString(" ")
            .trim()
    }

    fun stripWakeWord(raw: String): String {
        val prefixes = listOf(
            "hey finance vault",
            "ok finance vault",
            "hello finance vault",
            "finance vault",
            "hey vault",
            "ok vault",
            "hello vault",
            "hey gemini",
            "ok gemini"
        )
        var result = raw.trim()
        val lower = result.lowercase(Locale.US)
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                result = result.drop(p.length).trimStart(',', ' ', ':', '-', '.')
                break
            }
        }
        return result
    }

    private fun looksLikeFinancialQuery(text: String): Boolean {
        val lower = text.lowercase(Locale.US)
        val queryStarters = listOf(
            "what ", "how much", "show ", "tell me", "check ", "which ", "list ",
            "balance", "net worth", "total spent", "top place", "audit", "summary", "where "
        )
        return text.endsWith("?") || queryStarters.any { lower.contains(it) }
    }

    /**
     * Deterministic English NLP parser for instant hands-free transaction extraction.
     * Supports numeric digits ("450", "1,250.50") as well as spoken English number words ("five hundred", "two thousand").
     */
    fun parseEnglishTransactionLocally(
        rawText: String,
        uiState: FinanceVaultUiState
    ): ParsedVoiceTransaction? {
        val cleaned = stripWakeWord(rawText).trim()
        if (cleaned.isEmpty()) return null
        val lower = cleaned.lowercase(Locale.US)

        // Do not misclassify non-transaction creation or deletion commands as new transactions
        if (
            Regex("""\b(?:delete|remove|reverse|erase|cancel|undo|archive)\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower) ||
            lower.contains("create account") ||
            lower.contains("add account") ||
            lower.contains("subscription") ||
            lower.contains("budget") ||
            lower.contains("savings goal") ||
            lower.contains("create goal") ||
            lower.contains("gift card") ||
            lower.startsWith("invest ")
        ) {
            return null
        }

        val extractedAmount = extractAmountPaiseFromEnglish(cleaned)
        val hasExplicitTxVerb = Regex(
            """\b(?:spent|paid|bought|purchase|purchased|record\s+(?:an?\s+)?(?:expense|income|transfer|transaction)|add\s+(?:an?\s+)?(?:expense|income|transfer|transaction)|received|earned|credited|got\s+paid|deposited|transfer|transferred)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lower)

        if ((extractedAmount == null || extractedAmount <= 0L) && !hasExplicitTxVerb) {
            return null
        }
        val amountPaise = extractedAmount?.coerceAtLeast(0L) ?: 0L

        val txType = when {
            lower.contains("transfer") || (lower.contains("from ") && lower.contains(" to ") && !lower.contains("spent") && !lower.contains("paid")) ->
                TransactionType.TRANSFER
            lower.contains("received") || lower.contains("earned") || lower.contains("salary") ||
                lower.contains("income") || lower.contains("credited") || lower.contains("got paid") ||
                lower.contains("deposited") ->
                TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        val customCategories = uiState.customOptionsMap["TX_CATEGORY"].orEmpty()
        val explicitCategory = inferExplicitCategoryFromEnglish(lower, txType, customCategories)
        val isCategoryExplicit = explicitCategory != null
        val category = explicitCategory ?: inferCategoryFromEnglish(lower, txType, customCategories)
        val locationName = extractLocationOrMerchant(cleaned)
        val (srcHint, dstHint) = extractAccountHints(cleaned, txType, uiState)

        val cleanDescription = buildCleanNarration(cleaned, category, locationName, txType)
        val formattedRupees = if (amountPaise > 0L) InrMoney.formatPaise(amountPaise) else "the specified amount"
        val typeWord = when (txType) {
            TransactionType.INCOME -> "income"
            TransactionType.TRANSFER -> "transfer"
            else -> "expense"
        }
        val placeSuffix = if (locationName.isNotBlank()) " at $locationName" else ""
        val spokenConfirmation = "Recorded $typeWord of $formattedRupees for $category$placeSuffix."

        return ParsedVoiceTransaction(
            transactionType = txType,
            amountPaise = amountPaise,
            category = category,
            description = cleanDescription,
            locationName = locationName,
            sourceAccountHint = srcHint,
            destinationAccountHint = dstHint,
            spokenConfirmation = spokenConfirmation,
            isCategoryExplicit = isCategoryExplicit
        )
    }

    fun extractAmountPaiseFromEnglish(text: String): Long? {
        val normalized = convertSpokenEnglishNumbersToDigits(text)
        val multiplierRegex = Regex("""(?:₹|rs\.?|inr|\b)\s*(\d[\d,]*(?:\.\d{1,2})?)\s*(k|thousand|lakh|lakhs|lac|crore|crores)?\b""", RegexOption.IGNORE_CASE)
        val matches = multiplierRegex.findAll(normalized).toList()
        for (match in matches) {
            val rawNum = match.groupValues[1].replace(",", "")
            val value = rawNum.toDoubleOrNull() ?: continue
            if (value <= 0.0) continue
            val unit = match.groupValues.getOrNull(2)?.lowercase(Locale.US).orEmpty()
            val multiplier = when (unit) {
                "k", "thousand" -> 1_000.0
                "lakh", "lakhs", "lac" -> 100_000.0
                "crore", "crores" -> 10_000_000.0
                else -> 1.0
            }
            return (value * multiplier * 100.0).toLong()
        }
        return null
    }

    private fun convertSpokenEnglishNumbersToDigits(input: String): String {
        var s = input.lowercase(Locale.US)
        val phraseReplacements = listOf(
            "one hundred and fifty" to "150",
            "two hundred and fifty" to "250",
            "five hundred" to "500",
            "one hundred" to "100",
            "two hundred" to "200",
            "three hundred" to "300",
            "four hundred" to "400",
            "six hundred" to "600",
            "seven hundred" to "700",
            "eight hundred" to "800",
            "nine hundred" to "900",
            "one thousand" to "1000",
            "two thousand" to "2000",
            "five thousand" to "5000",
            "ten thousand" to "10000",
            "twenty five thousand" to "25000",
            "fifty thousand" to "50000",
            "one lakh" to "100000",
            "two lakh" to "200000",
            "five lakh" to "500000",
            "ten lakh" to "1000000",
            "fifty" to "50",
            "twenty" to "20",
            "thirty" to "30",
            "forty" to "40",
            "sixty" to "60",
            "seventy" to "70",
            "eighty" to "80",
            "ninety" to "90"
        )
        for ((words, digits) in phraseReplacements) {
            s = s.replace(Regex("\\b$words\\b"), digits)
        }
        return s
    }

    fun inferExplicitCategoryFromEnglish(
        lower: String,
        txType: TransactionType,
        customCategories: List<String>
    ): String? {
        for (custom in customCategories) {
            if (custom.isNotBlank() && lower.contains(custom.lowercase(Locale.US))) {
                return custom
            }
        }
        if (txType == TransactionType.TRANSFER) {
            return "Account Transfer"
        }
        if (txType == TransactionType.INCOME) {
            return when {
                lower.contains("salary") || lower.contains("paycheck") || lower.contains("stipend") || lower.contains("wages") -> "Salary Income"
                lower.contains("freelance") || lower.contains("client") || lower.contains("business") || lower.contains("consulting") || lower.contains("project") -> "Business & Freelance"
                lower.contains("rent") || lower.contains("tenant") -> "Rental Income"
                lower.contains("dividend") -> "Dividend Income"
                lower.contains("interest") -> "Interest Income"
                lower.contains("refund") || lower.contains("cashback") -> "Refund & Cashback"
                else -> null
            }
        }
        return when {
            lower.contains("coffee") || lower.contains("tea") || lower.contains("chai") ||
                lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") ||
                lower.contains("food") || lower.contains("restaurant") || lower.contains("cafe") ||
                lower.contains("snack") || lower.contains("swiggy") || lower.contains("zomato") ||
                lower.contains("starbucks") || lower.contains("pizza") || lower.contains("burger") ||
                lower.contains("dining") || lower.contains("meal") || lower.contains("biryani") ||
                lower.contains("bakery") -> "Food & Dining"
            lower.contains("grocery") || lower.contains("groceries") || lower.contains("vegetables") ||
                lower.contains("fruits") || lower.contains("milk") || lower.contains("supermarket") ||
                lower.contains("dmart") || lower.contains("blinkit") || lower.contains("zepto") ||
                lower.contains("bigbasket") || lower.contains("instamart") || lower.contains("household") ||
                lower.contains("kirana") || lower.contains("reliance fresh") -> "Household & Groceries"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") ||
                lower.contains("cng") || lower.contains("cab") || lower.contains("taxi") ||
                lower.contains("auto") || lower.contains("metro") || lower.contains("train") ||
                lower.contains("flight") || lower.contains("bus") || lower.contains("transport") ||
                lower.contains("toll") || lower.contains("parking") -> "Transportation & Fuel"
            lower.contains("electricity") || lower.contains("water") || lower.contains("wifi") ||
                lower.contains("broadband") || lower.contains("internet") || lower.contains("recharge") ||
                lower.contains("mobile bill") || lower.contains("phone bill") || lower.contains("gas") ||
                lower.contains("lpg") || lower.contains("cylinder") || lower.contains("utility") ||
                lower.contains("utilities") || lower.contains("bill") -> "Utilities & Recharge"
            lower.contains("rent") || lower.contains("maintenance") || lower.contains("housing") ||
                lower.contains("society") -> "Housing & Rent"
            lower.contains("medicine") || lower.contains("doctor") || lower.contains("hospital") ||
                lower.contains("pharmacy") || lower.contains("medical") || lower.contains("health") ||
                lower.contains("clinic") || lower.contains("apollo") || lower.contains("lab test") -> "Healthcare & Medical"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("movie") ||
                lower.contains("cinema") || lower.contains("subscription") || lower.contains("prime") ||
                lower.contains("youtube") || lower.contains("entertainment") || lower.contains("hotstar") ||
                lower.contains("gaming") -> "Entertainment & Subscriptions"
            lower.contains("book") || lower.contains("books") || lower.contains("course") ||
                lower.contains("tuition") || lower.contains("school") || lower.contains("college") ||
                lower.contains("education") || lower.contains("stationery") -> "Education"
            lower.contains("shopping") || lower.contains("amazon") || lower.contains("flipkart") ||
                lower.contains("myntra") || lower.contains("ajio") || lower.contains("clothes") ||
                lower.contains("shoes") || lower.contains("apparel") || lower.contains("mall") ||
                lower.contains("electronics") || lower.contains("gift") -> "Shopping & Lifestyle"
            else -> null
        }
    }

    fun inferCategoryFromEnglish(
        lower: String,
        txType: TransactionType,
        customCategories: List<String>
    ): String {
        inferExplicitCategoryFromEnglish(lower, txType, customCategories)?.let { return it }
        return when (txType) {
            TransactionType.INCOME -> "Salary Income"
            TransactionType.TRANSFER -> "Account Transfer"
            else -> "Household & Groceries"
        }
    }

    private fun extractLocationOrMerchant(text: String): String {
        val atRegex = Regex("""\b(?:at|@)\s+([A-Za-z0-9][A-Za-z0-9 &'.-]{1,32}?)(?=\s+\b(?:from|using|via|with|by|in|on|today|yesterday)\b|$)""", RegexOption.IGNORE_CASE)
        val match = atRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        return ""
    }

    private fun extractAccountHints(
        text: String,
        txType: TransactionType,
        uiState: FinanceVaultUiState
    ): Pair<String, String> {
        val accounts = uiState.accountsWithBalances.map { it.account }.filter { !it.isArchived }
        val lower = text.lowercase(Locale.US)

        if (txType == TransactionType.TRANSFER) {
            val transferRegex = Regex("""\bfrom\s+([A-Za-z0-9 ]+?)\s+to\s+([A-Za-z0-9 ]+?)$""", RegexOption.IGNORE_CASE)
            val m = transferRegex.find(text)
            if (m != null) {
                return m.groupValues[1].trim() to m.groupValues[2].trim()
            }
        }

        for (acc in accounts) {
            if (acc.name.isNotBlank() && lower.contains(acc.name.lowercase(Locale.US))) {
                return if (txType == TransactionType.INCOME) "" to acc.name else acc.name to ""
            }
            if (acc.bankName.isNotBlank() && lower.contains(acc.bankName.lowercase(Locale.US))) {
                return if (txType == TransactionType.INCOME) "" to acc.name else acc.name to ""
            }
        }

        val fromRegex = Regex("""\b(?:from|using|via|in|into)\s+([A-Za-z0-9 ]{2,25}?)(?:\s+account|\s+bank|\s+wallet|$)""", RegexOption.IGNORE_CASE)
        val m = fromRegex.find(text)
        val hint = m?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return if (txType == TransactionType.INCOME) "" to hint else hint to ""
    }

    /**
     * Generates a clean, normal transaction description without any "Voice Expense:" prefix
     * or raw spoken transcript clutter, matching manually entered transactions.
     */
    fun buildCleanNarration(
        rawText: String,
        category: String,
        locationName: String,
        txType: TransactionType
    ): String {
        val itemPhrase = Regex(
            """\b(?:on|for|towards)\s+([A-Za-z][A-Za-z0-9 &'-]{1,28}?)(?=\s+\b(?:at|from|using|via|with|in|into|to|by|today|yesterday|for\s+rs|for\s+₹|\d)\b|$)""",
            RegexOption.IGNORE_CASE
        ).find(rawText)?.groupValues?.getOrNull(1)?.trim()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            .orEmpty()

        val baseTitle = when {
            itemPhrase.isNotBlank() && !itemPhrase.equals("expense", ignoreCase = true) &&
                !itemPhrase.equals("income", ignoreCase = true) &&
                !itemPhrase.equals("transaction", ignoreCase = true) -> itemPhrase
            category.isNotBlank() -> category
            else -> txType.userLabel
        }

        return if (locationName.isNotBlank() && !baseTitle.contains(locationName, ignoreCase = true)) {
            "$baseTitle • $locationName"
        } else {
            baseTitle
        }
    }

    fun answerFinancialQueryLocally(query: String, uiState: FinanceVaultUiState): String {
        val lower = query.lowercase(Locale.US)
        val totalBalance = uiState.accountsWithBalances
            .filter { !it.account.isArchived }
            .sumOf { it.closingBalancePaise }
        val netWorth = uiState.balanceSheet.totalNetWorthPaise
        val monthSpent = uiState.currentMonthTotalSpentPaise
        val monthIncome = uiState.monthlyProfitAndLoss.totalIncomePaise

        return when {
            lower.contains("net worth") || lower.contains("balance sheet") -> {
                "Your current net worth is ${InrMoney.formatPaise(netWorth)}, with total assets of ${InrMoney.formatPaise(uiState.balanceSheet.totalAssetsPaise)} and liabilities of ${InrMoney.formatPaise(uiState.balanceSheet.totalLiabilitiesPaise)}."
            }
            lower.contains("profit") || lower.contains("p&l") || lower.contains("income") -> {
                "This month your income is ${InrMoney.formatPaise(monthIncome)} and your expenses are ${InrMoney.formatPaise(monthSpent)}."
            }
            lower.contains("spent") || lower.contains("expense") || lower.contains("spending") -> {
                val topPlace = uiState.transactions
                    .filter { !it.isReversed && it.transactionType == TransactionType.EXPENSE && it.locationName.isNotBlank() }
                    .groupBy { it.locationName }
                    .maxByOrNull { (_, list) -> list.sumOf { it.amountPaise } }
                val placeNote = if (topPlace != null) {
                    " Your highest spending place is ${topPlace.key} at ${InrMoney.formatPaise(topPlace.value.sumOf { it.amountPaise })}."
                } else ""
                "You have spent ${InrMoney.formatPaise(monthSpent)} this month across ${uiState.transactions.count { !it.isReversed }} transactions.$placeNote"
            }
            lower.contains("subscription") -> {
                val count = uiState.subscriptions.size
                if (count == 0) {
                    "You have no active subscriptions recorded in Finance Vault."
                } else {
                    val nextSub = uiState.subscriptions.minByOrNull { it.nextRenewalEpochDay }
                    "You have $count active subscriptions. Next renewal is ${nextSub?.name} on ${IndianFinancialYear.formatEpochDay(nextSub?.nextRenewalEpochDay ?: IndianFinancialYear.todayEpochDay())}."
                }
            }
            lower.contains("investment") || lower.contains("portfolio") -> {
                val totalInv = uiState.investments.sumOf { it.currentRecordedValuePaise ?: it.investedAmountPaise }
                "You have ${uiState.investments.size} investments valued at ${InrMoney.formatPaise(totalInv)}."
            }
            lower.contains("loan") || lower.contains("emi") -> {
                "You have ${uiState.loans.size} active loans with total outstanding liabilities of ${InrMoney.formatPaise(uiState.balanceSheet.totalLiabilitiesPaise)}."
            }
            lower.contains("budget") || lower.contains("goal") -> {
                "You have ${uiState.budgets.size} budgets and ${uiState.goals.size} savings goals configured."
            }
            else -> {
                "Across ${uiState.accountsWithBalances.count { !it.account.isArchived }} accounts, your total balance is ${InrMoney.formatPaise(totalBalance)}, monthly income is ${InrMoney.formatPaise(monthIncome)}, and monthly spending is ${InrMoney.formatPaise(monthSpent)}."
            }
        }
    }

    private fun buildVaultContextPrompt(uiState: FinanceVaultUiState): String {
        val accountsSummary = uiState.accountsWithBalances
            .filter { !it.account.isArchived }
            .joinToString("; ") { "${it.account.name} (${it.account.accountType.displayName}): ${InrMoney.formatPaise(it.closingBalancePaise)}" }
            .ifBlank { "No accounts created yet (will auto-create Primary Account on voice record)" }

        val recentTxSummary = uiState.transactions
            .filter { !it.isReversed }
            .take(10)
            .joinToString("; ") {
                "${IndianFinancialYear.formatEpochDay(it.dateEpochDay)} ${it.transactionType.name} ${InrMoney.formatPaise(it.amountPaise)} [${it.category}] ${it.locationName} - ${it.description}"
            }
            .ifBlank { "None" }

        val subscriptionsSummary = uiState.subscriptions
            .joinToString("; ") {
                "${it.name}: ${InrMoney.formatPaise(it.costPaise)} (${it.cycleLabel()}), next renewal ${IndianFinancialYear.formatEpochDay(it.nextRenewalEpochDay)}"
            }
            .ifBlank { "None" }

        val investmentsSummary = uiState.investments
            .take(5)
            .joinToString("; ") {
                "${it.name} (${it.category.displayName}): ${InrMoney.formatPaise(it.currentRecordedValuePaise ?: it.investedAmountPaise)}"
            }
            .ifBlank { "None" }

        val budgetsAndGoalsSummary =
            "Budgets: ${uiState.budgets.size}, Goals: ${uiState.goals.joinToString(", ") { "${it.name} (${InrMoney.formatPaise(it.currentSavedPaise)}/${InrMoney.formatPaise(it.targetAmountPaise)})" }.ifBlank { "None" }}"

        return """
            - Financial Year: ${uiState.settings.activeFinancialYear}
            - Net Worth: ${InrMoney.formatPaise(uiState.balanceSheet.totalNetWorthPaise)}
            - Total Assets: ${InrMoney.formatPaise(uiState.balanceSheet.totalAssetsPaise)}
            - Total Liabilities: ${InrMoney.formatPaise(uiState.balanceSheet.totalLiabilitiesPaise)}
            - This Month Income: ${InrMoney.formatPaise(uiState.monthlyProfitAndLoss.totalIncomePaise)}
            - This Month Expense: ${InrMoney.formatPaise(uiState.currentMonthTotalSpentPaise)}
            - Accounts: $accountsSummary
            - Subscriptions: $subscriptionsSummary
            - Investments: $investmentsSummary
            - $budgetsAndGoalsSummary
            - Recent Transactions: $recentTxSummary
        """.trimIndent()
    }
}
