package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.AppSettingsEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.BillingFrequency
import com.example.data.local.BudgetEntity
import com.example.data.local.BudgetPeriod
import com.example.data.local.CompoundingFrequency
import com.example.data.local.GiftCardEntity
import com.example.data.local.GoalEntity
import com.example.data.local.GoalType
import com.example.data.local.InvestmentCategory
import com.example.data.local.InvestmentEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.JournalLineEntity
import com.example.data.local.LedgerAccountEntity
import com.example.data.local.LoanEntity
import com.example.data.local.LoanPaymentEntity
import com.example.data.local.LoanType
import com.example.data.local.SubscriptionEntity
import com.example.data.local.TaxRecordEntity
import com.example.data.local.TaxType
import com.example.data.local.ThemeMode
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.FinanceVaultRepository
import com.example.domain.ai.GeminiVaultAssistant
import com.example.domain.ai.ParsedVoiceTransaction
import com.example.domain.ai.TransactionPatternAnalyzer
import com.example.domain.calculators.FinancialCalculators
import com.example.domain.export.DocumentExporter
import com.example.domain.location.LocationHelper
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.domain.reminder.TransactionRecallReminderManager
import com.example.domain.reports.FinancialReportGenerator
import com.example.domain.security.PinSecurityUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class MainTab(val title: String, val testTag: String) {
    HOME("Home", "tab_home"),
    ACCOUNTS("Accounts", "tab_accounts"),
    ACTIVITY("Activity", "tab_activity"),
    INVESTMENTS("Investments", "tab_investments"),
    MORE("More", "tab_more")
}

enum class MoreDestination(val title: String, val subtitle: String, val testTag: String) {
    HUB("More Hub", "", "more_hub"),
    REPORTS("Financial Statements & Ledgers", "", "more_reports"),
    ANALYTICS("Analytics & Performance", "", "more_analytics"),
    LOANS("Loans & Credit Cards", "", "more_loans"),
    BUDGETS_GOALS("Budgets & Savings Goals", "", "more_budgets_goals"),
    GIFT_CARDS("Gift Card", "", "more_gift_cards"),
    SUBSCRIPTIONS("Subscriptions & Renewals", "", "more_subscriptions"),
    TAXES("Indian Tax Records", "", "more_taxes"),
    SETTINGS("Settings, Backup & About", "", "more_settings")
}

enum class ReportTab(val userLabel: String, val accountingSubtitle: String) {
    BALANCE_SHEET("Balance Sheet", ""),
    PROFIT_AND_LOSS("Profit and Loss Account", ""),
    CASH_FLOW("Cash Flow Statement", ""),
    TRIAL_BALANCE("Trial Balance", ""),
    ACCOUNT_LEDGER("General Ledger", ""),
    JOURNAL_BOOK("Journal Book", ""),
    SCHEDULES("Schedules to Accounts", "")
}

enum class BudgetAlertTier(val thresholdLevel: Int, val badgeLabel: String) {
    NORMAL(0, "On Track"),
    REACHED_80(80, "80% Threshold Reached"),
    REACHED_90(90, "90% Critical Alert"),
    EXCEEDED_100(100, "Budget Exceeded")
}

@Immutable
data class BudgetThresholdStatus(
    val budget: BudgetEntity,
    val isOverall: Boolean,
    val spentPaise: Long,
    val limitPaise: Long,
    val usagePercent: Double,
    val tier: BudgetAlertTier
)

@Immutable
data class AccountWithBalance(
    val account: AccountEntity,
    val ledgerAccount: LedgerAccountEntity?,
    val closingBalancePaise: Long,
    val totalDebitsPaise: Long,
    val totalCreditsPaise: Long
)

private val EMPTY_BALANCE_SHEET = FinancialReportGenerator.generateBalanceSheet(emptyList(), emptyList())
private val EMPTY_PROFIT_AND_LOSS = FinancialReportGenerator.generateProfitAndLoss(emptyList(), emptyList())
private val EMPTY_CASH_FLOW = FinancialReportGenerator.generateCashFlowStatement(emptyList(), emptyList())
private val EMPTY_TRIAL_BALANCE = FinancialReportGenerator.generateTrialBalance(emptyList(), emptyList())
private val EMPTY_JOURNAL_BOOK = FinancialReportGenerator.generateJournalBook(emptyList(), emptyList())

@Immutable
data class FinanceVaultUiState(
    val isInitializing: Boolean = true,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val isLockedByPin: Boolean = false,
    val accountsWithBalances: List<AccountWithBalance> = emptyList(),
    val ledgerAccounts: List<LedgerAccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val journalLines: List<JournalLineEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanPayments: List<LoanPaymentEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetStatuses: List<BudgetThresholdStatus> = emptyList(),
    val overallMonthlyBudgetStatus: BudgetThresholdStatus? = null,
    val currentMonthTotalSpentPaise: Long = 0L,
    val customOptionsMap: Map<String, List<String>> = emptyMap(),
    val goals: List<GoalEntity> = emptyList(),
    val giftCards: List<GiftCardEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val taxRecords: List<TaxRecordEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    // Derived Reports from Single Source of Truth (Journal & Ledgers)
    val balanceSheet: FinancialReportGenerator.BalanceSheetReport = EMPTY_BALANCE_SHEET,
    val fyProfitAndLoss: FinancialReportGenerator.ProfitAndLossReport = EMPTY_PROFIT_AND_LOSS,
    val monthlyProfitAndLoss: FinancialReportGenerator.ProfitAndLossReport = EMPTY_PROFIT_AND_LOSS,
    val fyCashFlow: FinancialReportGenerator.CashFlowReport = EMPTY_CASH_FLOW,
    val trialBalance: FinancialReportGenerator.TrialBalanceReport = EMPTY_TRIAL_BALANCE,
    val journalBook: FinancialReportGenerator.JournalBookReport = EMPTY_JOURNAL_BOOK,
    val monthlySavingsRatePercent: Double = 0.0,
    val fySavingsRatePercent: Double = 0.0,
    val hasAnyUserRecords: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceVaultViewModel(
    private val repository: FinanceVaultRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _moreDestination = MutableStateFlow(MoreDestination.HUB)
    val moreDestination: StateFlow<MoreDestination> = _moreDestination.asStateFlow()

    private val _selectedReportTab = MutableStateFlow<ReportTab?>(null)
    val selectedReportTab: StateFlow<ReportTab?> = _selectedReportTab.asStateFlow()

    private val _selectedLedgerAccountId = MutableStateFlow<Long?>(null)
    val selectedLedgerAccountId: StateFlow<Long?> = _selectedLedgerAccountId.asStateFlow()

    private val _showAddTransactionSheet = MutableStateFlow(false)
    val showAddTransactionSheet: StateFlow<Boolean> = _showAddTransactionSheet.asStateFlow()

    private val _showGeminiAssistantDialog = MutableStateFlow(false)
    val showGeminiAssistantDialog: StateFlow<Boolean> = _showGeminiAssistantDialog.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _exportPreviewResult = MutableStateFlow<DocumentExporter.ExportedDocumentResult?>(null)
    val exportPreviewResult: StateFlow<DocumentExporter.ExportedDocumentResult?> = _exportPreviewResult.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    @Immutable
    data class VoiceDisambiguationOption(
        val index: Int,
        val label: String,
        val subtitle: String = "",
        val entityId: Long = 0L
    )

    @Immutable
    data class PendingVoiceDisambiguation(
        val title: String,
        val questionPrompt: String,
        val options: List<VoiceDisambiguationOption>,
        val onSelectOption: suspend (VoiceDisambiguationOption) -> String,
        val onSpokenAmountProvided: (suspend (Long) -> String)? = null
    )

    private sealed interface UndoableVoiceAction {
        val description: String
        data class CreatedTransaction(val transactionId: Long, override val description: String) : UndoableVoiceAction
        data class ReversedTransaction(val snapshot: TransactionEntity, override val description: String) : UndoableVoiceAction
        data class CreatedSubscription(val subscriptionId: Long, override val description: String) : UndoableVoiceAction
        data class DeletedSubscription(val snapshot: SubscriptionEntity, override val description: String) : UndoableVoiceAction
        data class CreatedBudget(val budgetId: Long, override val description: String) : UndoableVoiceAction
        data class DeletedBudget(val snapshot: BudgetEntity, override val description: String) : UndoableVoiceAction
        data class CreatedGoal(val goalId: Long, override val description: String) : UndoableVoiceAction
        data class DeletedGoal(val snapshot: GoalEntity, override val description: String) : UndoableVoiceAction
        data class ArchivedAccount(val accountId: Long, override val description: String) : UndoableVoiceAction
    }

    private val _pendingVoiceDisambiguation = MutableStateFlow<PendingVoiceDisambiguation?>(null)
    val pendingVoiceDisambiguation: StateFlow<PendingVoiceDisambiguation?> = _pendingVoiceDisambiguation.asStateFlow()

    private var lastUndoableVoiceAction: UndoableVoiceAction? = null
    private var activeVoiceCommandJob: Job? = null

    private val _isPinUnlockedSession = MutableStateFlow(false)
    private val notifiedBudgetTiers = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialized()
        }
    }

    private val settingsFlow = repository.observeSettings()
        .map { it ?: AppSettingsEntity() }
        .distinctUntilChanged()

    private val isDemoFlow = settingsFlow
        .map { it.isDemoModeEnabled }
        .distinctUntilChanged()

    private val coreAccountingFlow = isDemoFlow.flatMapLatest { isDemo ->
        combine(
            repository.observeAccounts(isDemo),
            repository.observeLedgerAccounts(isDemo),
            repository.observeTransactions(isDemo),
            repository.observeJournalEntries(isDemo),
            repository.observeJournalLines(isDemo)
        ) { accounts, ledgers, txs, entries, lines ->
            CoreAccountingBundle(accounts, ledgers, txs, entries, lines)
        }
    }

    private val secondaryEntitiesFlow = isDemoFlow.flatMapLatest { isDemo ->
        combine(
            repository.observeInvestments(isDemo),
            repository.observeLoans(isDemo),
            repository.observeLoanPayments(isDemo),
            repository.observeBudgets(isDemo),
            repository.observeGoals(isDemo)
        ) { investments, loans, loanPayments, budgets, goals ->
            SecondaryBundle1(investments, loans, loanPayments, budgets, goals)
        }
    }

    private val tertiaryEntitiesFlow = isDemoFlow.flatMapLatest { isDemo ->
        combine(
            repository.observeGiftCards(isDemo),
            repository.observeSubscriptions(isDemo),
            repository.observeTaxRecords(isDemo),
            repository.observeAuditLogs(isDemo)
        ) { giftCards, subscriptions, taxes, logs ->
            SecondaryBundle2(giftCards, subscriptions, taxes, logs)
        }
    }

    val uiState: StateFlow<FinanceVaultUiState> = combine(
        settingsFlow,
        _isPinUnlockedSession,
        coreAccountingFlow,
        secondaryEntitiesFlow,
        tertiaryEntitiesFlow
    ) { rawSettings, unlocked, core, sec1, sec2 ->
        val systemFy = IndianFinancialYear.currentFyLabel()
        val settings = rawSettings.copy(activeFinancialYear = systemFy)
        val (fyStart, fyEnd) = IndianFinancialYear.getRangeForFyLabel(systemFy)
        val (monthStart, monthEnd) = IndianFinancialYear.currentMonthRange()

        val linesByLedgerId = core.lines.groupBy { it.ledgerAccountId }
        val ledgerByUserAccId = core.ledgers
            .filter { it.linkedUserAccountId != null }
            .associateBy { it.linkedUserAccountId!! }

        val accountsWithBalances = core.accounts.map { acc ->
            val ledger = ledgerByUserAccId[acc.id]
            val accLines = if (ledger != null) linesByLedgerId[ledger.id].orEmpty() else emptyList()
            val debits = accLines.sumOf { it.debitPaise }
            val credits = accLines.sumOf { it.creditPaise }
            val closing = com.example.domain.accounting.AccountingEngine.calculateClosingBalance(
                normalBalance = acc.accountType.normalBalance,
                openingBalancePaise = 0L,
                totalDebitsPaise = debits,
                totalCreditsPaise = credits
            )
            AccountWithBalance(
                account = acc,
                ledgerAccount = ledger,
                closingBalancePaise = closing,
                totalDebitsPaise = debits,
                totalCreditsPaise = credits
            )
        }

        val balanceSheet = FinancialReportGenerator.generateBalanceSheet(
            ledgerAccounts = core.ledgers,
            allLines = core.lines
        )
        val fyPnl = FinancialReportGenerator.generateProfitAndLoss(
            ledgerAccounts = core.ledgers,
            allLines = core.lines,
            fromEpochDay = fyStart,
            toEpochDay = fyEnd
        )
        val monthlyPnl = FinancialReportGenerator.generateProfitAndLoss(
            ledgerAccounts = core.ledgers,
            allLines = core.lines,
            fromEpochDay = monthStart,
            toEpochDay = monthEnd
        )
        val fyCashFlow = FinancialReportGenerator.generateCashFlowStatement(
            ledgerAccounts = core.ledgers,
            allLines = core.lines,
            fromEpochDay = fyStart,
            toEpochDay = fyEnd
        )
        val trialBalance = FinancialReportGenerator.generateTrialBalance(
            ledgerAccounts = core.ledgers,
            allLines = core.lines
        )
        val journalBook = FinancialReportGenerator.generateJournalBook(
            entries = core.entries,
            lines = core.lines,
            fromEpochDay = fyStart,
            toEpochDay = fyEnd
        )

        val monthlySavingsRate = FinancialCalculators.calculateSavingsRate(
            incomePaise = monthlyPnl.totalIncomePaise,
            expensesPaise = monthlyPnl.totalExpensesPaise
        )
        val fySavingsRate = FinancialCalculators.calculateSavingsRate(
            incomePaise = fyPnl.totalIncomePaise,
            expensesPaise = fyPnl.totalExpensesPaise
        )

        val hasPin = !settings.pinHashSha256.isNullOrBlank()
        val hasSecurityLock = hasPin || settings.isBiometricEnabled
        val hasRecords = core.accounts.isNotEmpty() ||
            core.txs.isNotEmpty() ||
            sec1.investments.isNotEmpty() ||
            sec1.loans.isNotEmpty() ||
            sec2.giftCards.isNotEmpty()

        val expenseTypes = setOf(
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PURCHASE,
            TransactionType.GIFT_CARD_USAGE
        )
        val activeMonthExpenseTxs = core.txs.filter {
            !it.isReversed && it.dateEpochDay in monthStart..monthEnd && it.transactionType in expenseTypes
        }
        val activeFyExpenseTxs = core.txs.filter {
            !it.isReversed && it.dateEpochDay in fyStart..fyEnd && it.transactionType in expenseTypes
        }
        val currentMonthTotalSpentPaise = activeMonthExpenseTxs.sumOf { it.amountPaise }
        val currentFyTotalSpentPaise = activeFyExpenseTxs.sumOf { it.amountPaise }

        val allBudgetStatuses = sec1.budgets.map { b ->
            val isOverall = b.category.equals("OVERALL", ignoreCase = true)
            val spentPaise = if (isOverall) {
                if (b.budgetPeriod == BudgetPeriod.MONTHLY) currentMonthTotalSpentPaise else currentFyTotalSpentPaise
            } else {
                val pool = if (b.budgetPeriod == BudgetPeriod.MONTHLY) activeMonthExpenseTxs else activeFyExpenseTxs
                pool.filter { it.category.equals(b.category, ignoreCase = true) }.sumOf { it.amountPaise }
            }
            val pct = if (b.limitPaise > 0L) (spentPaise.toDouble() / b.limitPaise.toDouble()) * 100.0 else 0.0
            val tier = when {
                pct >= 100.0 -> BudgetAlertTier.EXCEEDED_100
                pct >= 90.0 -> BudgetAlertTier.REACHED_90
                pct >= 80.0 -> BudgetAlertTier.REACHED_80
                else -> BudgetAlertTier.NORMAL
            }
            BudgetThresholdStatus(
                budget = b,
                isOverall = isOverall,
                spentPaise = spentPaise,
                limitPaise = b.limitPaise,
                usagePercent = pct,
                tier = tier
            )
        }

        // Trigger non-intrusive 80% and 90% threshold notifications for monthly budgets (Overall & Category)
        for (status in allBudgetStatuses.filter { it.budget.budgetPeriod == BudgetPeriod.MONTHLY }) {
            if (status.usagePercent >= 80.0) {
                val alertLevel = if (status.usagePercent >= 90.0) 90 else 80
                val key = "${settings.isDemoModeEnabled}_${status.budget.id}_${status.budget.category.uppercase()}"
                val prevLevel = notifiedBudgetTiers[key] ?: 0
                if (alertLevel > prevLevel) {
                    notifiedBudgetTiers[key] = alertLevel
                    val pctStr = String.format(java.util.Locale.US, "%.0f", status.usagePercent)
                    val targetName = if (status.isOverall) "Overall Monthly Budget" else status.budget.category
                    _statusBannerMessage.value = "Budget Alert ($alertLevel%): $targetName reached $pctStr% of monthly budget."
                }
            }
        }

        val overallMonthlyStatus = allBudgetStatuses.firstOrNull {
            it.isOverall && it.budget.budgetPeriod == BudgetPeriod.MONTHLY
        }
        val categoryBudgetStatuses = allBudgetStatuses.filter { !it.isOverall }
        val customOptionsMap = FinanceVaultRepository.parseCustomOptionsMap(settings.customCategoriesJson)

        FinanceVaultUiState(
            isInitializing = false,
            settings = settings,
            isLockedByPin = hasSecurityLock && !unlocked,
            accountsWithBalances = accountsWithBalances,
            ledgerAccounts = core.ledgers,
            transactions = core.txs,
            journalEntries = core.entries,
            journalLines = core.lines,
            investments = sec1.investments,
            loans = sec1.loans,
            loanPayments = sec1.loanPayments,
            budgets = sec1.budgets,
            budgetStatuses = categoryBudgetStatuses,
            overallMonthlyBudgetStatus = overallMonthlyStatus,
            currentMonthTotalSpentPaise = currentMonthTotalSpentPaise,
            customOptionsMap = customOptionsMap,
            goals = sec1.goals,
            giftCards = sec2.giftCards,
            subscriptions = sec2.subscriptions,
            taxRecords = sec2.taxes,
            auditLogs = sec2.logs,
            balanceSheet = balanceSheet,
            fyProfitAndLoss = fyPnl,
            monthlyProfitAndLoss = monthlyPnl,
            fyCashFlow = fyCashFlow,
            trialBalance = trialBalance,
            journalBook = journalBook,
            monthlySavingsRatePercent = monthlySavingsRate,
            fySavingsRatePercent = fySavingsRate,
            hasAnyUserRecords = hasRecords
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceVaultUiState(isInitializing = true)
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearchQuery(rawQuery: String = _searchQuery.value) {
        val clean = rawQuery.trim()
        if (clean.isEmpty()) return
        _searchQuery.value = clean
        val updated = listOf(clean) + _searchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }
        _searchHistory.value = updated.take(5)
    }

    fun selectSearchHistoryTerm(term: String) {
        val clean = term.trim()
        if (clean.isEmpty()) return
        _searchQuery.value = clean
        val updated = listOf(clean) + _searchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }
        _searchHistory.value = updated.take(5)
    }

    fun removeSearchHistoryTerm(term: String) {
        _searchHistory.value = _searchHistory.value.filterNot { it.equals(term, ignoreCase = true) }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun toggleSearchExpanded(expanded: Boolean? = null) {
        val next = expanded ?: !_isSearchExpanded.value
        _isSearchExpanded.value = next
        if (!next && _searchQuery.value.isNotBlank()) {
            commitSearchQuery(_searchQuery.value)
        }
    }

    /**
     * Auto-closes the search bar and dropdown menu while preserving the active search query
     * and search results on screen until the user navigates elsewhere in the app.
     */
    fun collapseSearchBar() {
        if (_searchQuery.value.isNotBlank()) {
            commitSearchQuery(_searchQuery.value)
        }
        _isSearchExpanded.value = false
    }

    /**
     * Completely closes the search bar and clears the active search results (called on navigation).
     */
    fun closeAndResetSearch() {
        if (_searchQuery.value.isNotBlank()) {
            commitSearchQuery(_searchQuery.value)
        }
        _isSearchExpanded.value = false
        _searchQuery.value = ""
    }

    fun selectTab(tab: MainTab) {
        if (_isSearchExpanded.value || _searchQuery.value.isNotBlank()) {
            closeAndResetSearch()
        }
        _selectedTab.value = tab
        if (tab != MainTab.MORE) {
            _moreDestination.value = MoreDestination.HUB
        }
    }

    fun navigateMore(destination: MoreDestination) {
        if (_isSearchExpanded.value || _searchQuery.value.isNotBlank()) {
            closeAndResetSearch()
        }
        _selectedTab.value = MainTab.MORE
        _moreDestination.value = destination
    }

    fun selectReportTab(tab: ReportTab?) {
        _selectedReportTab.value = tab
    }

    fun selectLedgerAccount(id: Long?) {
        _selectedLedgerAccountId.value = id
    }

    fun openAddTransactionSheet(open: Boolean) {
        if (open && (_isSearchExpanded.value || _searchQuery.value.isNotBlank())) {
            closeAndResetSearch()
        }
        _showAddTransactionSheet.value = open
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun dismissExportPreview() {
        _exportPreviewResult.value = null
    }

    fun attemptPinUnlock(enteredPin: String): Boolean {
        val s = uiState.value.settings
        val valid = PinSecurityUtil.verifyPin(enteredPin, s.pinHashSha256, s.pinSalt)
        if (valid) {
            _isPinUnlockedSession.value = true
        }
        return valid
    }

    fun verifyCurrentPin(enteredPin: String): Boolean {
        val s = uiState.value.settings
        return PinSecurityUtil.verifyPin(enteredPin, s.pinHashSha256, s.pinSalt)
    }

    fun lockAppNow() {
        if (!uiState.value.settings.pinHashSha256.isNullOrBlank()) {
            _isPinUnlockedSession.value = false
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = uiState.value.settings.isPrivacyModeEnabled
            repository.togglePrivacyMode(!current)
        }
    }

    fun toggleDemoMode(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDemonstrationMode(enable)
            _statusBannerMessage.value = if (enable) {
                "Demonstration Mode enabled: Showing isolated [DEMO - FICTIONAL] records."
            } else {
                "Switched to your real Finance Vault records."
            }
        }
    }

    fun createAccount(
        name: String,
        accountType: AccountType,
        bankName: String,
        branchName: String,
        accountNumber: String,
        ifscCode: String,
        accountHolder: String,
        upiId: String,
        openingBalancePaise: Long,
        isOpeningBalanceRecorded: Boolean,
        creditLimitPaise: Long?,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.createAccount(
                    name = name,
                    accountType = accountType,
                    bankName = bankName,
                    branchName = branchName,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    accountHolder = accountHolder,
                    upiId = upiId,
                    openingBalancePaise = openingBalancePaise,
                    isOpeningBalanceRecorded = isOpeningBalanceRecorded,
                    creditLimitPaise = creditLimitPaise,
                    notes = notes,
                    isDemo = uiState.value.settings.isDemoModeEnabled
                )
            }.onSuccess {
                _statusBannerMessage.value = "Account '$name' added and Chart of Accounts updated."
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not add account: ${e.message}"
            }
        }
    }

    fun archiveAccount(accountId: Long, archive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.archiveAccount(accountId, archive)
        }
    }

    fun recordTransaction(
        transactionType: TransactionType,
        dateEpochDay: Long,
        amountPaise: Long,
        principalComponentPaise: Long = 0L,
        interestComponentPaise: Long = 0L,
        chargesPaise: Long = 0L,
        costBasisDisposedPaise: Long = 0L,
        sourceAccountId: Long? = null,
        destinationAccountId: Long? = null,
        category: String,
        description: String,
        referenceNumber: String = "",
        notes: String = "",
        locationName: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        linkedInvestmentId: Long? = null,
        linkedLoanId: Long? = null,
        linkedGiftCardId: Long? = null,
        linkedOriginalTransactionId: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.recordTransaction(
                    transactionType = transactionType,
                    dateEpochDay = dateEpochDay,
                    amountPaise = amountPaise,
                    principalComponentPaise = principalComponentPaise,
                    interestComponentPaise = interestComponentPaise,
                    chargesPaise = chargesPaise,
                    costBasisDisposedPaise = costBasisDisposedPaise,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destinationAccountId,
                    category = category,
                    description = description,
                    referenceNumber = referenceNumber,
                    notes = notes,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    linkedInvestmentId = linkedInvestmentId,
                    linkedLoanId = linkedLoanId,
                    linkedGiftCardId = linkedGiftCardId,
                    linkedOriginalTransactionId = linkedOriginalTransactionId,
                    isDemo = uiState.value.settings.isDemoModeEnabled
                )
            }.onSuccess {
                _showAddTransactionSheet.value = false
                _statusBannerMessage.value = "Transaction recorded & double-entry ledgers updated."
            }.onFailure { e ->
                _statusBannerMessage.value = "Validation error: ${e.message}"
            }
        }
    }

    fun reverseTransaction(transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.reverseTransaction(transactionId)
            }.onSuccess {
                _statusBannerMessage.value = "Reversal entry posted and balances updated."
            }.onFailure { e ->
                _statusBannerMessage.value = "Reversal failed: ${e.message}"
            }
        }
    }

    fun reverseTransactionsBulk(transactionIds: Set<Long>) {
        if (transactionIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.reverseTransactionsBulk(transactionIds)
            }.onSuccess { count ->
                _statusBannerMessage.value = if (count > 0) {
                    "Reversed $count transaction(s) in ledger history and updated balances."
                } else {
                    "Selected transaction(s) are already reversed."
                }
            }.onFailure { e ->
                _statusBannerMessage.value = "Bulk reversal failed: ${e.message}"
            }
        }
    }

    fun categorizeTransactionsBulk(transactionIds: Set<Long>, newCategory: String) {
        if (transactionIds.isEmpty() || newCategory.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.categorizeTransactionsBulk(transactionIds, newCategory)
            }.onSuccess { count ->
                _statusBannerMessage.value = "Categorized $count transaction(s) under '$newCategory'."
            }.onFailure { e ->
                _statusBannerMessage.value = "Bulk categorization failed: ${e.message}"
            }
        }
    }

    fun addInvestment(
        name: String,
        category: InvestmentCategory,
        purchaseDateEpochDay: Long,
        quantity: Double,
        units: Double,
        purchasePricePerUnitPaise: Long,
        investedAmountPaise: Long,
        chargesPaise: Long,
        currentRecordedValuePaise: Long?,
        isCurrentValueRecorded: Boolean,
        paymentAccountId: Long?,
        annualInterestRatePercent: Double?,
        compoundingFrequency: CompoundingFrequency,
        durationMonths: Int?,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val maturityEpoch = if (durationMonths != null && durationMonths > 0) {
                    purchaseDateEpochDay + (durationMonths * 30L)
                } else null
                repository.addInvestment(
                    name = name,
                    category = category,
                    purchaseDateEpochDay = purchaseDateEpochDay,
                    quantity = quantity,
                    units = units,
                    purchasePricePerUnitPaise = purchasePricePerUnitPaise,
                    investedAmountPaise = investedAmountPaise,
                    chargesPaise = chargesPaise,
                    currentRecordedValuePaise = currentRecordedValuePaise,
                    isCurrentValueRecorded = isCurrentValueRecorded,
                    paymentAccountId = paymentAccountId,
                    maturityDateEpochDay = maturityEpoch,
                    annualInterestRatePercent = annualInterestRatePercent,
                    compoundingFrequency = compoundingFrequency,
                    durationMonths = durationMonths,
                    notes = notes,
                    isDemo = uiState.value.settings.isDemoModeEnabled
                )
            }.onSuccess {
                _statusBannerMessage.value = "Investment '$name' recorded in portfolio & Balance Sheet."
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not add investment: ${e.message}"
            }
        }
    }

    fun updateInvestmentValuation(investmentId: Long, recordedValuePaise: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInvestmentRecordedValue(investmentId, recordedValuePaise)
            _statusBannerMessage.value = "Recorded investment valuation updated."
        }
    }

    fun addLoan(
        name: String,
        loanType: LoanType,
        lenderName: String,
        principalPaise: Long,
        annualInterestRatePercent: Double,
        tenureMonths: Int,
        startDateEpochDay: Long,
        disbursedToAccountId: Long?,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.addLoan(
                    name = name,
                    loanType = loanType,
                    lenderName = lenderName,
                    principalPaise = principalPaise,
                    annualInterestRatePercent = annualInterestRatePercent,
                    tenureMonths = tenureMonths,
                    startDateEpochDay = startDateEpochDay,
                    disbursedToAccountId = disbursedToAccountId,
                    notes = notes,
                    isDemo = uiState.value.settings.isDemoModeEnabled
                )
            }.onSuccess {
                _statusBannerMessage.value = "Loan '$name' & EMI schedule added to liabilities."
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not add loan: ${e.message}"
            }
        }
    }

    fun addBudget(
        name: String,
        budgetPeriod: BudgetPeriod,
        category: String,
        limitPaise: Long,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBudget(
                name = name,
                budgetPeriod = budgetPeriod,
                category = category,
                limitPaise = limitPaise,
                financialYear = uiState.value.settings.activeFinancialYear,
                notes = notes,
                isDemo = uiState.value.settings.isDemoModeEnabled
            )
            _statusBannerMessage.value = "Budget '$name' saved."
        }
    }

    fun saveOverallMonthlyBudget(limitPaise: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBudget(
                name = "Overall Monthly Budget",
                budgetPeriod = BudgetPeriod.MONTHLY,
                category = "OVERALL",
                limitPaise = limitPaise,
                financialYear = uiState.value.settings.activeFinancialYear,
                notes = "",
                isDemo = uiState.value.settings.isDemoModeEnabled
            )
            _statusBannerMessage.value = "Overall monthly budget updated."
        }
    }

    fun addCustomOption(scopeKey: String, customValue: String) {
        val clean = customValue.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCustomCategoryOption(scopeKey, clean)
            _statusBannerMessage.value = "Added '$clean' to options."
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteBudget(id) }
    }

    fun addGoal(
        name: String,
        goalType: GoalType,
        targetAmountPaise: Long,
        currentSavedPaise: Long,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addGoal(
                name = name,
                goalType = goalType,
                targetAmountPaise = targetAmountPaise,
                currentSavedPaise = currentSavedPaise,
                notes = notes,
                isDemo = uiState.value.settings.isDemoModeEnabled
            )
            _statusBannerMessage.value = "Goal '$name' created."
        }
    }

    fun contributeToGoal(goalId: Long, amountPaise: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.contributeToGoal(goalId, amountPaise)
            _statusBannerMessage.value = "Goal progress updated."
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteGoal(id) }
    }

    fun addGiftCard(
        brand: String,
        name: String,
        initialValuePaise: Long,
        remainingValuePaise: Long,
        cardNumberRaw: String,
        redemptionPinRaw: String,
        purchasedFromAccountId: Long?,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.addGiftCard(
                    brand = brand,
                    name = name,
                    initialValuePaise = initialValuePaise,
                    remainingValuePaise = remainingValuePaise,
                    purchaseDateEpochDay = IndianFinancialYear.todayEpochDay(),
                    cardNumberRaw = cardNumberRaw,
                    redemptionPinRaw = redemptionPinRaw,
                    purchasedFromAccountId = purchasedFromAccountId,
                    notes = notes,
                    isDemo = uiState.value.settings.isDemoModeEnabled
                )
            }.onSuccess {
                _statusBannerMessage.value = "Gift card '$brand' added to assets."
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not add gift card: ${e.message}"
            }
        }
    }

    fun addSubscription(
        name: String,
        costPaise: Long,
        billingFrequency: BillingFrequency,
        nextRenewalEpochDay: Long,
        paymentAccountId: Long?,
        notes: String,
        customIntervalCount: Int = 1,
        customIntervalUnit: String = "MONTHS"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSubscription(
                name = name,
                costPaise = costPaise,
                billingFrequency = billingFrequency,
                nextRenewalEpochDay = nextRenewalEpochDay,
                paymentAccountId = paymentAccountId,
                notes = notes,
                isDemo = uiState.value.settings.isDemoModeEnabled,
                customIntervalCount = customIntervalCount,
                customIntervalUnit = customIntervalUnit
            )
            _statusBannerMessage.value = "Subscription '$name' scheduled for ${IndianFinancialYear.formatEpochDay(nextRenewalEpochDay)}."
        }
    }

    fun updateSubscriptionRenewalSchedule(
        subscriptionId: Long,
        nextRenewalEpochDay: Long,
        billingFrequency: BillingFrequency,
        customIntervalCount: Int = 1,
        customIntervalUnit: String = "MONTHS"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSubscriptionRenewalSchedule(
                subscriptionId = subscriptionId,
                nextRenewalEpochDay = nextRenewalEpochDay,
                billingFrequency = billingFrequency,
                customIntervalCount = customIntervalCount,
                customIntervalUnit = customIntervalUnit
            )
            _statusBannerMessage.value = "Updated renewal date to ${IndianFinancialYear.formatEpochDay(nextRenewalEpochDay)}."
        }
    }

    fun recordSubscriptionPaymentNow(
        subscriptionId: Long,
        sourceAccountId: Long,
        nextCustomRenewalEpochDay: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.recordSubscriptionPaymentNow(
                    subscriptionId = subscriptionId,
                    sourceAccountId = sourceAccountId,
                    nextCustomRenewalEpochDay = nextCustomRenewalEpochDay
                )
            }.onSuccess {
                _statusBannerMessage.value = "Subscription payment recorded & next renewal updated."
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not record payment: ${e.message}"
            }
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteSubscription(id) }
    }

    fun openGeminiAssistantDialog(show: Boolean) {
        _showGeminiAssistantDialog.value = show
    }

    @Immutable
    data class AssistantFulfillmentPayload(
        val spokenReply: String,
        val netWorthFormatted: String,
        val totalBalanceFormatted: String,
        val monthlySpendingFormatted: String,
        val monthlyIncomeFormatted: String,
        val recordedTransactionsCount: Int,
        val errorMessage: String? = null
    )

    /**
     * Builds a Schema.org JSON-LD payload and populates Android AssistData Bundle so Google Assistant
     * can read relevant financial information directly when invoked by the user.
     */
    fun buildAssistContentJsonLd(): String {
        val s = uiState.value
        val isHidden = s.settings.isPrivacyModeEnabled
        val totalBalancePaise = s.accountsWithBalances
            .filter { !it.account.isArchived }
            .sumOf { it.closingBalancePaise }
        val totalBalanceStr = InrMoney.formatPaise(totalBalancePaise, isHidden = isHidden)
        val netWorthStr = InrMoney.formatPaise(s.balanceSheet.totalNetWorthPaise, isHidden = isHidden)
        val monthSpentStr = InrMoney.formatPaise(s.currentMonthTotalSpentPaise, isHidden = isHidden)
        val monthIncomeStr = InrMoney.formatPaise(s.monthlyProfitAndLoss.totalIncomePaise, isHidden = isHidden)

        val accountsList = s.accountsWithBalances
            .filter { !it.account.isArchived }
            .joinToString(", ") {
                "${it.account.name}: ${InrMoney.formatPaise(it.closingBalancePaise, isHidden = isHidden)}"
            }
            .ifBlank { "No active accounts" }

        return org.json.JSONObject().apply {
            put("@context", "https://schema.org")
            put("@type", "AccountingService")
            put("name", "Finance Vault")
            put("description", "Personal Financial Summary for ${s.settings.activeFinancialYear}")
            put("netWorth", netWorthStr)
            put("totalAccountBalance", totalBalanceStr)
            put("currentMonthSpending", monthSpentStr)
            put("currentMonthIncome", monthIncomeStr)
            put("accountsSummary", accountsList)
            put("activeSubscriptionsCount", s.subscriptions.size)
        }.toString()
    }

    fun populateAssistBundle(bundle: android.os.Bundle) {
        val s = uiState.value
        val isHidden = s.settings.isPrivacyModeEnabled
        val totalBalancePaise = s.accountsWithBalances
            .filter { !it.account.isArchived }
            .sumOf { it.closingBalancePaise }
        bundle.putString("app_name", "Finance Vault")
        bundle.putString("financial_year", s.settings.activeFinancialYear)
        bundle.putString("net_worth_inr", InrMoney.formatPaise(s.balanceSheet.totalNetWorthPaise, isHidden = isHidden))
        bundle.putString("total_balance_inr", InrMoney.formatPaise(totalBalancePaise, isHidden = isHidden))
        bundle.putString("monthly_spending_inr", InrMoney.formatPaise(s.currentMonthTotalSpentPaise, isHidden = isHidden))
        bundle.putString("monthly_income_inr", InrMoney.formatPaise(s.monthlyProfitAndLoss.totalIncomePaise, isHidden = isHidden))
        bundle.putInt("active_subscriptions_count", s.subscriptions.size)
    }

    fun dismissVoiceDisambiguation() {
        _pendingVoiceDisambiguation.value = null
    }

    fun cancelActiveVoiceCommand() {
        activeVoiceCommandJob?.cancel()
        activeVoiceCommandJob = null
        _pendingVoiceDisambiguation.value = null
    }

    fun selectVoiceDisambiguationOption(
        option: VoiceDisambiguationOption,
        onFinished: (String, String?, Boolean) -> Unit = { _, _, _ -> }
    ) {
        val pending = _pendingVoiceDisambiguation.value ?: return
        _pendingVoiceDisambiguation.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                pending.onSelectOption(option)
            }.onSuccess { reply ->
                _statusBannerMessage.value = reply
                val hasNextQuestion = _pendingVoiceDisambiguation.value != null
                onFinished(reply, null, hasNextQuestion)
            }.onFailure { e ->
                val err = "Could not complete selected option: ${e.message}"
                _statusBannerMessage.value = err
                onFinished(err, e.localizedMessage, false)
            }
        }
    }

    private fun matchDisambiguationOptionFromSpeech(
        utterance: String,
        options: List<VoiceDisambiguationOption>
    ): VoiceDisambiguationOption? {
        val lower = utterance.lowercase(java.util.Locale.US).trim()
        // Check explicit option numbers or ordinals ("1", "option 1", "first", "number one")
        val numberWords = mapOf(
            "1" to 1, "one" to 1, "first" to 1, "1st" to 1,
            "2" to 2, "two" to 2, "second" to 2, "2nd" to 2,
            "3" to 3, "three" to 3, "third" to 3, "3rd" to 3,
            "4" to 4, "four" to 4, "fourth" to 4, "4th" to 4,
            "5" to 5, "five" to 5, "fifth" to 5, "5th" to 5
        )
        for ((word, idx) in numberWords) {
            if (lower == word || lower == "option $word" || lower == "number $word" || lower == "the $word one") {
                options.firstOrNull { it.index == idx }?.let { return it }
            }
        }
        // Check label substring match
        for (opt in options) {
            val optLower = opt.label.lowercase(java.util.Locale.US)
            if (lower.contains(optLower) || optLower.contains(lower)) {
                return opt
            }
            val optTokens = optLower.split(" ").filter { it.length >= 3 }
            if (optTokens.any { token -> lower.contains(token) }) {
                return opt
            }
        }
        return null
    }

    private fun applyNavigationTargets(
        tab: MainTab?,
        more: MoreDestination?,
        report: ReportTab?,
        ledgerHint: String,
        currentState: FinanceVaultUiState
    ) {
        if (report != null) {
            _selectedTab.value = MainTab.MORE
            _moreDestination.value = MoreDestination.REPORTS
            _selectedReportTab.value = report
            if (report == ReportTab.ACCOUNT_LEDGER && ledgerHint.isNotBlank()) {
                val matchedLedger = currentState.ledgerAccounts.firstOrNull {
                    it.name.contains(ledgerHint, ignoreCase = true)
                }
                if (matchedLedger != null) {
                    _selectedLedgerAccountId.value = matchedLedger.id
                }
            }
        } else if (more != null) {
            _selectedTab.value = MainTab.MORE
            _moreDestination.value = more
        } else if (tab != null) {
            _selectedTab.value = tab
        }
    }

    /**
     * Evaluates a parsed voice transaction and sequentially prompts the user for any missing
     * required slot (1. Amount, 2. Category, 3. Account when multiple options exist) before
     * committing the transaction with live high-precision GPS street, area, and coordinates.
     *
     * @return Pair(spokenReply, needsFollowUpMicrophone)
     */
    private suspend fun processVoiceTransactionWithSlotFilling(
        context: Context,
        utterance: String,
        tx: com.example.domain.ai.ParsedVoiceTransaction,
        capturedGps: com.example.domain.location.CapturedLocation?,
        isDemo: Boolean
    ): Pair<String, Boolean> {
        val currentState = uiState.value
        val activeAccounts = currentState.accountsWithBalances
            .map { it.account }
            .filter { !it.isArchived }

        val pattern = TransactionPatternAnalyzer.analyzeForTransaction(
            utterance = utterance,
            fallbackType = tx.transactionType,
            fallbackCategory = tx.category,
            explicitLocationName = tx.locationName,
            explicitSourceAccountHint = tx.sourceAccountHint,
            explicitDestinationAccountHint = tx.destinationAccountHint,
            transactions = currentState.transactions,
            activeAccounts = activeAccounts
        )

        val resolvedType = pattern.inferredTransactionType
        val isCategoryFulfilled = tx.isCategoryExplicit ||
            pattern.isCategoryAutoSelectedFromHistory ||
            resolvedType == TransactionType.TRANSFER
        val resolvedCategory = pattern.inferredCategory
        val merchantOrPlace = tx.locationName.ifBlank { pattern.inferredLocationName }
        val resolvedLocation = LocationHelper.mergeLocationWithLiveGps(merchantOrPlace, capturedGps)

        // Clean description without any voice transcript clutter
        val cleanDescription = if (tx.description.isBlank() || tx.description.startsWith("Voice ", ignoreCase = true)) {
            GeminiVaultAssistant.buildCleanNarration(utterance, resolvedCategory, merchantOrPlace, resolvedType)
        } else {
            tx.description
        }

        // SLOT 1: Missing Amount -> Ask the user how much the transaction was for!
        if (tx.amountPaise <= 0L) {
            val quickAmounts = listOf(100_00L, 250_00L, 500_00L, 1_000_00L, 2_500_00L)
            val options = quickAmounts.mapIndexed { idx, paise ->
                VoiceDisambiguationOption(
                    index = idx + 1,
                    label = InrMoney.formatPaise(paise),
                    subtitle = "Quick amount option",
                    entityId = paise
                )
            }
            val typeWord = when (resolvedType) {
                TransactionType.INCOME -> "income"
                TransactionType.TRANSFER -> "transfer"
                else -> "expense"
            }
            val catHint = if (isCategoryFulfilled) " for $resolvedCategory" else ""
            val question = "How much was this $typeWord$catHint? Please say the amount in rupees or pick an option."

            _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                title = "Enter Amount for ${resolvedType.userLabel}",
                questionPrompt = question,
                options = options,
                onSelectOption = { chosen ->
                    val updatedTx = tx.copy(amountPaise = chosen.entityId)
                    processVoiceTransactionWithSlotFilling(
                        context = context,
                        utterance = utterance,
                        tx = updatedTx,
                        capturedGps = capturedGps,
                        isDemo = isDemo
                    ).first
                },
                onSpokenAmountProvided = { spokenPaise ->
                    val updatedTx = tx.copy(amountPaise = spokenPaise)
                    processVoiceTransactionWithSlotFilling(
                        context = context,
                        utterance = utterance,
                        tx = updatedTx,
                        capturedGps = capturedGps,
                        isDemo = isDemo
                    ).first
                }
            )
            return question to true
        }

        // SLOT 2: Missing / Unfulfilled Category -> Ask the user which category to use!
        if (!isCategoryFulfilled) {
            val customCats = currentState.customOptionsMap["TX_CATEGORY"].orEmpty()
            val candidateCategories = if (resolvedType == TransactionType.INCOME) {
                (listOf("Salary Income", "Business & Freelance", "Rental Income", "Interest Income", "Refund & Cashback") + customCats).distinct().take(6)
            } else {
                (listOf(
                    "Food & Dining",
                    "Household & Groceries",
                    "Transportation & Fuel",
                    "Utilities & Recharge",
                    "Shopping & Lifestyle",
                    "Healthcare & Medical"
                ) + customCats).distinct().take(6)
            }
            val options = candidateCategories.mapIndexed { idx, cat ->
                VoiceDisambiguationOption(
                    index = idx + 1,
                    label = cat,
                    subtitle = "${resolvedType.userLabel} Category",
                    entityId = (idx + 1).toLong()
                )
            }
            val amtFormatted = InrMoney.formatPaise(tx.amountPaise)
            val optionsSpoken = options.take(5).joinToString(", or ") { "${it.index}. ${it.label}" }
            val question = "Which category should I use for $amtFormatted: $optionsSpoken?"

            _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                title = "Select Category for $amtFormatted",
                questionPrompt = question,
                options = options,
                onSelectOption = { chosen ->
                    val chosenCat = chosen.label
                    val updatedDescription = GeminiVaultAssistant.buildCleanNarration(
                        utterance,
                        chosenCat,
                        merchantOrPlace,
                        resolvedType
                    )
                    val updatedTx = tx.copy(
                        category = chosenCat,
                        description = updatedDescription,
                        isCategoryExplicit = true
                    )
                    processVoiceTransactionWithSlotFilling(
                        context = context,
                        utterance = utterance,
                        tx = updatedTx,
                        capturedGps = capturedGps,
                        isDemo = isDemo
                    ).first
                }
            )
            return question to true
        }

        // SLOT 3: Multiple Account Options with no single learned/explicit account -> Ask which account to use!
        if (pattern.requiresAccountDisambiguation && pattern.candidateAccounts.size >= 2) {
            val options = pattern.candidateAccounts.take(5).mapIndexed { idx, acc ->
                val bal = currentState.accountsWithBalances.firstOrNull { it.account.id == acc.id }?.closingBalancePaise ?: 0L
                VoiceDisambiguationOption(
                    index = idx + 1,
                    label = acc.name,
                    subtitle = "${acc.accountType.displayName} • ${InrMoney.formatPaise(bal)}",
                    entityId = acc.id
                )
            }
            val optionsSpokenList = options.joinToString(", or ") { "${it.index}. ${it.label}" }
            val amtFormatted = InrMoney.formatPaise(tx.amountPaise)
            val question = "Which account should I use for $amtFormatted ($resolvedCategory): $optionsSpokenList?"

            _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                title = "Select Account for $amtFormatted ($resolvedCategory)",
                questionPrompt = question,
                options = options,
                onSelectOption = { chosen ->
                    val liveGpsNow = capturedGps ?: withTimeoutOrNull(2_500L) {
                        LocationHelper.fetchCurrentLocation(context)
                    }
                    val finalLocation = LocationHelper.mergeLocationWithLiveGps(merchantOrPlace, liveGpsNow)
                    val txId = if (resolvedType == TransactionType.INCOME) {
                        repository.recordTransaction(
                            transactionType = TransactionType.INCOME,
                            dateEpochDay = IndianFinancialYear.todayEpochDay(),
                            amountPaise = tx.amountPaise,
                            destinationAccountId = chosen.entityId,
                            category = resolvedCategory,
                            description = cleanDescription,
                            locationName = finalLocation,
                            latitude = liveGpsNow?.latitude,
                            longitude = liveGpsNow?.longitude,
                            isDemo = isDemo
                        )
                    } else {
                        repository.recordTransaction(
                            transactionType = TransactionType.EXPENSE,
                            dateEpochDay = IndianFinancialYear.todayEpochDay(),
                            amountPaise = tx.amountPaise,
                            sourceAccountId = chosen.entityId,
                            category = resolvedCategory,
                            description = cleanDescription,
                            locationName = finalLocation,
                            latitude = liveGpsNow?.latitude,
                            longitude = liveGpsNow?.longitude,
                            isDemo = isDemo
                        )
                    }
                    lastUndoableVoiceAction = UndoableVoiceAction.CreatedTransaction(
                        transactionId = txId,
                        description = "$amtFormatted in $resolvedCategory"
                    )
                    TransactionRecallReminderManager.dismissInAppRecallPrompt(context)
                    val gpsNote = if (!liveGpsNow?.streetAndArea.isNullOrBlank()) " at ${liveGpsNow?.streetAndArea}" else ""
                    "Recorded ${if (resolvedType == TransactionType.INCOME) "income" else "expense"} of $amtFormatted for $resolvedCategory using ${chosen.label}$gpsNote."
                }
            )
            return question to true
        }

        // ALL SLOTS FULFILLED: Record transaction immediately with live high-accuracy GPS!
        val liveGpsNow = capturedGps ?: withTimeoutOrNull(2_500L) {
            LocationHelper.fetchCurrentLocation(context)
        }
        val finalLocation = LocationHelper.mergeLocationWithLiveGps(merchantOrPlace, liveGpsNow)

        val confirmation = runCatching {
            when (resolvedType) {
                TransactionType.INCOME -> {
                    val dstAcc = pattern.preferredDestinationAccount
                        ?: repository.resolveOrCreateAccountForVoice(
                            spokenAccountHint = tx.destinationAccountHint.ifBlank { tx.sourceAccountHint },
                            isDemo = isDemo
                        )
                    val txId = repository.recordTransaction(
                        transactionType = TransactionType.INCOME,
                        dateEpochDay = IndianFinancialYear.todayEpochDay(),
                        amountPaise = tx.amountPaise,
                        destinationAccountId = dstAcc.id,
                        category = resolvedCategory,
                        description = cleanDescription,
                        locationName = finalLocation,
                        latitude = liveGpsNow?.latitude,
                        longitude = liveGpsNow?.longitude,
                        isDemo = isDemo
                    )
                    lastUndoableVoiceAction = UndoableVoiceAction.CreatedTransaction(
                        transactionId = txId,
                        description = "${InrMoney.formatPaise(tx.amountPaise)} income in $resolvedCategory"
                    )
                    TransactionRecallReminderManager.dismissInAppRecallPrompt(context)
                    val autoNote = if (pattern.isAccountAutoSelectedFromHistory) {
                        " into ${dstAcc.name} (auto-selected from your past transactions)"
                    } else " into ${dstAcc.name}"
                    "Recorded income of ${InrMoney.formatPaise(tx.amountPaise)} for $resolvedCategory$autoNote."
                }

                TransactionType.TRANSFER -> {
                    val srcAcc = pattern.preferredSourceAccount
                        ?: repository.resolveOrCreateAccountForVoice(
                            spokenAccountHint = tx.sourceAccountHint,
                            isDemo = isDemo
                        )
                    val dstAcc = pattern.preferredDestinationAccount
                        ?: repository.resolveOrCreateAccountForVoice(
                            spokenAccountHint = tx.destinationAccountHint.ifBlank { "Cash Wallet" },
                            isDemo = isDemo
                        )
                    val finalDstId = if (dstAcc.id == srcAcc.id) {
                        repository.createAccount(
                            name = tx.destinationAccountHint.ifBlank { "Secondary Account" },
                            accountType = AccountType.CASH,
                            openingBalancePaise = 0L,
                            isOpeningBalanceRecorded = false,
                            openingDateEpochDay = IndianFinancialYear.todayEpochDay(),
                            isDemo = isDemo
                        )
                    } else {
                        dstAcc.id
                    }
                    val txId = repository.recordTransaction(
                        transactionType = TransactionType.TRANSFER,
                        dateEpochDay = IndianFinancialYear.todayEpochDay(),
                        amountPaise = tx.amountPaise,
                        sourceAccountId = srcAcc.id,
                        destinationAccountId = finalDstId,
                        category = "Account Transfer",
                        description = cleanDescription,
                        locationName = finalLocation,
                        latitude = liveGpsNow?.latitude,
                        longitude = liveGpsNow?.longitude,
                        isDemo = isDemo
                    )
                    lastUndoableVoiceAction = UndoableVoiceAction.CreatedTransaction(
                        transactionId = txId,
                        description = "${InrMoney.formatPaise(tx.amountPaise)} transfer"
                    )
                    TransactionRecallReminderManager.dismissInAppRecallPrompt(context)
                    "Recorded transfer of ${InrMoney.formatPaise(tx.amountPaise)} from ${srcAcc.name}."
                }

                else -> {
                    val srcAcc = pattern.preferredSourceAccount
                        ?: repository.resolveOrCreateAccountForVoice(
                            spokenAccountHint = tx.sourceAccountHint,
                            isDemo = isDemo
                        )
                    val txId = repository.recordTransaction(
                        transactionType = TransactionType.EXPENSE,
                        dateEpochDay = IndianFinancialYear.todayEpochDay(),
                        amountPaise = tx.amountPaise,
                        sourceAccountId = srcAcc.id,
                        category = resolvedCategory,
                        description = cleanDescription,
                        locationName = finalLocation,
                        latitude = liveGpsNow?.latitude,
                        longitude = liveGpsNow?.longitude,
                        isDemo = isDemo
                    )
                    lastUndoableVoiceAction = UndoableVoiceAction.CreatedTransaction(
                        transactionId = txId,
                        description = "${InrMoney.formatPaise(tx.amountPaise)} expense in $resolvedCategory"
                    )
                    TransactionRecallReminderManager.dismissInAppRecallPrompt(context)
                    val placeSpoken = liveGpsNow?.streetAndArea?.takeIf { it.isNotBlank() } ?: merchantOrPlace
                    val placePart = if (placeSpoken.isNotBlank()) " at $placeSpoken" else ""
                    val autoNote = if (pattern.isAccountAutoSelectedFromHistory) {
                        " from ${srcAcc.name} (auto-selected from your past transactions)"
                    } else " from ${srcAcc.name}"
                    "Recorded expense of ${InrMoney.formatPaise(tx.amountPaise)} for $resolvedCategory$placePart$autoNote."
                }
            }
        }.getOrElse { e ->
            "Could not record transaction: ${e.message}"
        }

        return confirmation to false
    }

    /**
     * Full-App Omni-Voice & Google Assistant Command Executor.
     * Supports:
     * 1. Resolving pending multi-option disambiguation questions by voice.
     * 2. Smart Historical Transaction Pattern Learning (auto-selecting account, category, and location
     *    when similar transactions occurred previously at different amounts/times).
     * 3. Interactive Multi-Option Disambiguation when multiple accounts/subscriptions/loans/goals exist.
     * 4. Full navigation and entity creation across every screen and module, plus instant Voice Undo.
     */
    fun executeVoiceOrGeminiCommand(
        context: Context,
        utterance: String,
        onFinished: (String, String?, Boolean) -> Unit = { _, _, _ -> },
        onAssistantFulfillment: (AssistantFulfillmentPayload) -> Unit = {}
    ) {
        activeVoiceCommandJob?.cancel()
        activeVoiceCommandJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmedUtterance = utterance.trim()
                val lowerUtterance = trimmedUtterance.lowercase(java.util.Locale.US)

                // STEP 1: Check if we are waiting for the user to answer a Multi-Option or Missing-Slot question!
                val activeDisambiguation = _pendingVoiceDisambiguation.value
                if (activeDisambiguation != null) {
                    if (lowerUtterance == "cancel" || lowerUtterance == "never mind" || lowerUtterance == "stop") {
                        _pendingVoiceDisambiguation.value = null
                        val cancelMsg = "Cancelled pending selection."
                        _statusBannerMessage.value = cancelMsg
                        onFinished(cancelMsg, null, false)
                        return@launch
                    }

                    // If we are waiting for a missing amount and the user spoke any rupee amount, fulfill it directly!
                    val spokenAmtPaise = GeminiVaultAssistant.extractAmountPaiseFromEnglish(trimmedUtterance)
                    if (activeDisambiguation.onSpokenAmountProvided != null && spokenAmtPaise != null && spokenAmtPaise > 0L) {
                        _pendingVoiceDisambiguation.value = null
                        val reply = runCatching {
                            activeDisambiguation.onSpokenAmountProvided.invoke(spokenAmtPaise)
                        }.getOrElse { e ->
                            "Could not record amount: ${e.message}"
                        }
                        _statusBannerMessage.value = reply
                        val hasNextFollowUp = _pendingVoiceDisambiguation.value != null
                        onFinished(reply, null, hasNextFollowUp)
                        return@launch
                    }

                    val matchedOption = matchDisambiguationOptionFromSpeech(trimmedUtterance, activeDisambiguation.options)
                    if (matchedOption != null) {
                        _pendingVoiceDisambiguation.value = null
                        val reply = runCatching {
                            activeDisambiguation.onSelectOption(matchedOption)
                        }.getOrElse { e ->
                            "Could not complete option: ${e.message}"
                        }
                        _statusBannerMessage.value = reply
                        val hasNextFollowUp = _pendingVoiceDisambiguation.value != null
                        onFinished(reply, null, hasNextFollowUp)
                        return@launch
                    } else {
                        val wordsCount = trimmedUtterance.split(Regex("\\s+")).size
                        if (wordsCount <= 4 && spokenAmtPaise == null &&
                            !Regex("""\b(?:delete|remove|open|show|go|create|add|undo)\b""", RegexOption.IGNORE_CASE).containsMatchIn(lowerUtterance)
                        ) {
                            val retryPrompt = if (activeDisambiguation.onSpokenAmountProvided != null) {
                                "Please say the amount in rupees, or pick one of the options: " +
                                    activeDisambiguation.options.joinToString(", ") { it.label } + "."
                            } else {
                                "Please choose one of the options: " +
                                    activeDisambiguation.options.joinToString(", ") { "${it.index}. ${it.label}" } +
                                    ", or say Cancel."
                            }
                            _statusBannerMessage.value = retryPrompt
                            onFinished(retryPrompt, null, true)
                            return@launch
                        } else {
                            _pendingVoiceDisambiguation.value = null
                        }
                    }
                }

                // STEP 2: Interpret the Omni-Voice command
                val currentState = uiState.value
                val isDemo = currentState.settings.isDemoModeEnabled
                val result = GeminiVaultAssistant.interpretVoiceOrGeminiCommand(
                    utterance = trimmedUtterance,
                    uiState = currentState
                )

                // Apply any screen navigation requested or implied by the command
                applyNavigationTargets(
                    tab = result.targetTab,
                    more = result.targetMore,
                    report = result.targetReport,
                    ledgerHint = result.targetLedgerAccountHint,
                    currentState = currentState
                )

                var finalSpokenReply = result.spokenReply
                var listenForFollowUp = false

                when (result.action) {
                    "RECORD_TRANSACTION" -> {
                        val capturedGps = withTimeoutOrNull(4_200L) {
                            LocationHelper.fetchCurrentLocation(context)
                        }
                        for (tx in result.transactionsToRecord) {
                            val (replyText, needsFollowUp) = processVoiceTransactionWithSlotFilling(
                                context = context,
                                utterance = trimmedUtterance,
                                tx = tx,
                                capturedGps = capturedGps,
                                isDemo = isDemo
                            )
                            finalSpokenReply = replyText
                            if (needsFollowUp) {
                                listenForFollowUp = true
                                break
                            }
                        }
                    }

                "DELETE_TRANSACTION", "REVERSE_LAST_TRANSACTION" -> {
                    val activeTxs = currentState.transactions.filter { !it.isReversed }
                    if (activeTxs.isEmpty()) {
                        finalSpokenReply = "There are no active transactions to delete."
                    } else {
                        val isExplicitLatest = result.action == "REVERSE_LAST_TRANSACTION" ||
                            result.frequencyOrTypeHint.equals("LATEST", ignoreCase = true) ||
                            (result.entityName.isBlank() && result.primaryAmountPaise <= 0L)

                        if (isExplicitLatest || activeTxs.size == 1) {
                            val targetTx = activeTxs.first()
                            repository.reverseTransaction(targetTx.id)
                            lastUndoableVoiceAction = UndoableVoiceAction.ReversedTransaction(
                                snapshot = targetTx,
                                description = "${InrMoney.formatPaise(targetTx.amountPaise)} (${targetTx.category})"
                            )
                            finalSpokenReply = "Deleted transaction of ${InrMoney.formatPaise(targetTx.amountPaise)} for ${targetTx.category}. Say Undo to restore it."
                        } else {
                            val keyword = result.entityName.trim()
                            val amtFilter = result.primaryAmountPaise
                            val matchedTxs = activeTxs.filter { tx ->
                                val amtMatches = amtFilter <= 0L || tx.amountPaise == amtFilter
                                val keywordMatches = keyword.isBlank() ||
                                    tx.category.contains(keyword, ignoreCase = true) ||
                                    tx.description.contains(keyword, ignoreCase = true) ||
                                    tx.locationName.contains(keyword, ignoreCase = true) ||
                                    keyword.split(" ").filter { it.length >= 3 }.any { tok ->
                                        tx.category.contains(tok, ignoreCase = true) ||
                                            tx.description.contains(tok, ignoreCase = true) ||
                                            tx.locationName.contains(tok, ignoreCase = true)
                                    }
                                amtMatches && keywordMatches
                            }

                            when {
                                matchedTxs.size == 1 -> {
                                    val targetTx = matchedTxs.first()
                                    repository.reverseTransaction(targetTx.id)
                                    lastUndoableVoiceAction = UndoableVoiceAction.ReversedTransaction(
                                        snapshot = targetTx,
                                        description = "${InrMoney.formatPaise(targetTx.amountPaise)} (${targetTx.category})"
                                    )
                                    finalSpokenReply = "Deleted transaction of ${InrMoney.formatPaise(targetTx.amountPaise)} for ${targetTx.category}. Say Undo to restore it."
                                }
                                matchedTxs.size > 1 -> {
                                    val options = matchedTxs.take(5).mapIndexed { idx, tx ->
                                        val place = if (tx.locationName.isNotBlank()) " @ ${tx.locationName}" else ""
                                        VoiceDisambiguationOption(
                                            index = idx + 1,
                                            label = "${InrMoney.formatPaise(tx.amountPaise)} • ${tx.category}$place",
                                            subtitle = IndianFinancialYear.formatEpochDay(tx.dateEpochDay),
                                            entityId = tx.id
                                        )
                                    }
                                    val q = "Multiple transactions match. Which one should I delete: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                                    _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                                        title = "Select Transaction to Delete",
                                        questionPrompt = q,
                                        options = options,
                                        onSelectOption = { chosen ->
                                            val tx = currentState.transactions.firstOrNull { it.id == chosen.entityId }
                                            if (tx != null) {
                                                lastUndoableVoiceAction = UndoableVoiceAction.ReversedTransaction(
                                                    snapshot = tx,
                                                    description = "${InrMoney.formatPaise(tx.amountPaise)} (${tx.category})"
                                                )
                                            }
                                            repository.reverseTransaction(chosen.entityId)
                                            "Deleted transaction ${chosen.label}. Say Undo to restore it."
                                        }
                                    )
                                    finalSpokenReply = q
                                    listenForFollowUp = true
                                }
                                else -> {
                                    // No exact keyword match: ask from the most recent transactions
                                    val options = activeTxs.take(5).mapIndexed { idx, tx ->
                                        val place = if (tx.locationName.isNotBlank()) " @ ${tx.locationName}" else ""
                                        VoiceDisambiguationOption(
                                            index = idx + 1,
                                            label = "${InrMoney.formatPaise(tx.amountPaise)} • ${tx.category}$place",
                                            subtitle = IndianFinancialYear.formatEpochDay(tx.dateEpochDay),
                                            entityId = tx.id
                                        )
                                    }
                                    val q = "Which recent transaction should I delete: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                                    _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                                        title = "Select Transaction to Delete",
                                        questionPrompt = q,
                                        options = options,
                                        onSelectOption = { chosen ->
                                            val tx = currentState.transactions.firstOrNull { it.id == chosen.entityId }
                                            if (tx != null) {
                                                lastUndoableVoiceAction = UndoableVoiceAction.ReversedTransaction(
                                                    snapshot = tx,
                                                    description = "${InrMoney.formatPaise(tx.amountPaise)} (${tx.category})"
                                                )
                                            }
                                            repository.reverseTransaction(chosen.entityId)
                                            "Deleted transaction ${chosen.label}. Say Undo to restore it."
                                        }
                                    )
                                    finalSpokenReply = q
                                    listenForFollowUp = true
                                }
                            }
                        }
                    }
                }

                "UNDO_LAST_ACTION" -> {
                    val undoable = lastUndoableVoiceAction
                    lastUndoableVoiceAction = null
                    when (undoable) {
                        is UndoableVoiceAction.CreatedTransaction -> {
                            repository.reverseTransaction(undoable.transactionId)
                            finalSpokenReply = "Undid transaction: ${undoable.description}."
                        }
                        is UndoableVoiceAction.ReversedTransaction -> {
                            val snap = undoable.snapshot
                            repository.recordTransaction(
                                transactionType = snap.transactionType,
                                dateEpochDay = snap.dateEpochDay,
                                amountPaise = snap.amountPaise,
                                principalComponentPaise = snap.principalComponentPaise,
                                interestComponentPaise = snap.interestComponentPaise,
                                chargesPaise = snap.chargesPaise,
                                costBasisDisposedPaise = snap.costBasisDisposedPaise,
                                sourceAccountId = snap.sourceAccountId,
                                destinationAccountId = snap.destinationAccountId,
                                category = snap.category,
                                description = snap.description,
                                referenceNumber = snap.referenceNumber,
                                notes = snap.notes,
                                locationName = snap.locationName,
                                latitude = snap.latitude,
                                longitude = snap.longitude,
                                isDemo = snap.isDemo
                            )
                            finalSpokenReply = "Restored transaction: ${undoable.description}."
                        }
                        is UndoableVoiceAction.CreatedSubscription -> {
                            repository.deleteSubscription(undoable.subscriptionId)
                            finalSpokenReply = "Removed subscription: ${undoable.description}."
                        }
                        is UndoableVoiceAction.DeletedSubscription -> {
                            val sub = undoable.snapshot
                            repository.addSubscription(
                                name = sub.name,
                                costPaise = sub.costPaise,
                                billingFrequency = sub.billingFrequency,
                                nextRenewalEpochDay = sub.nextRenewalEpochDay,
                                paymentAccountId = sub.paymentAccountId,
                                notes = sub.notes,
                                isDemo = sub.isDemo,
                                customIntervalCount = sub.customIntervalCount,
                                customIntervalUnit = sub.customIntervalUnit
                            )
                            finalSpokenReply = "Restored subscription: ${undoable.description}."
                        }
                        is UndoableVoiceAction.CreatedBudget -> {
                            repository.deleteBudget(undoable.budgetId)
                            finalSpokenReply = "Removed budget: ${undoable.description}."
                        }
                        is UndoableVoiceAction.DeletedBudget -> {
                            val b = undoable.snapshot
                            repository.addBudget(
                                name = b.name,
                                budgetPeriod = b.budgetPeriod,
                                category = b.category,
                                limitPaise = b.limitPaise,
                                financialYear = b.financialYear,
                                notes = b.notes,
                                isDemo = b.isDemo
                            )
                            finalSpokenReply = "Restored budget: ${undoable.description}."
                        }
                        is UndoableVoiceAction.CreatedGoal -> {
                            repository.deleteGoal(undoable.goalId)
                            finalSpokenReply = "Removed goal: ${undoable.description}."
                        }
                        is UndoableVoiceAction.DeletedGoal -> {
                            val g = undoable.snapshot
                            repository.addGoal(
                                name = g.name,
                                goalType = g.goalType,
                                targetAmountPaise = g.targetAmountPaise,
                                currentSavedPaise = g.currentSavedPaise,
                                targetDateEpochDay = g.targetDateEpochDay,
                                linkedAccountId = g.linkedAccountId,
                                notes = g.notes,
                                isDemo = g.isDemo
                            )
                            finalSpokenReply = "Restored savings goal: ${undoable.description}."
                        }
                        is UndoableVoiceAction.ArchivedAccount -> {
                            repository.archiveAccount(undoable.accountId, false)
                            finalSpokenReply = "Restored account: ${undoable.description}."
                        }
                        null -> {
                            finalSpokenReply = "There is no recent voice action to undo."
                        }
                    }
                }

                "CREATE_ACCOUNT" -> {
                    val accType = when (result.frequencyOrTypeHint.uppercase(java.util.Locale.US)) {
                        "CASH" -> AccountType.CASH
                        "UPI" -> AccountType.UPI
                        "UPI_WALLET", "WALLET", "DIGITAL_WALLET" -> AccountType.DIGITAL_WALLET
                        "CREDIT_CARD" -> AccountType.CREDIT_CARD
                        "SAVINGS" -> AccountType.SAVINGS
                        "CURRENT" -> AccountType.CURRENT
                        "DEMAT", "OTHER" -> AccountType.OTHER
                        else -> AccountType.BANK
                    }
                    val accName = result.entityName.ifBlank { "New ${accType.displayName}" }
                    repository.createAccount(
                        name = accName,
                        accountType = accType,
                        bankName = if (accType == AccountType.BANK) accName else "",
                        openingBalancePaise = result.primaryAmountPaise,
                        isOpeningBalanceRecorded = result.primaryAmountPaise > 0L,
                        openingDateEpochDay = IndianFinancialYear.todayEpochDay(),
                        isDemo = isDemo
                    )
                }

                "ARCHIVE_ACCOUNT" -> {
                    val activeAccs = currentState.accountsWithBalances.map { it.account }.filter { !it.isArchived }
                    val matched = activeAccs.filter {
                        result.entityName.isNotBlank() && it.name.contains(result.entityName, ignoreCase = true)
                    }
                    if (activeAccs.isEmpty()) {
                        finalSpokenReply = "There are no active accounts to delete or archive."
                    } else if (matched.size == 1 || (activeAccs.size == 1 && result.entityName.isBlank())) {
                        val target = matched.firstOrNull() ?: activeAccs.first()
                        repository.archiveAccount(target.id, true)
                        lastUndoableVoiceAction = UndoableVoiceAction.ArchivedAccount(target.id, target.name)
                        finalSpokenReply = "Archived account ${target.name}. Say Undo to restore it."
                    } else {
                        val candidates = if (matched.isNotEmpty()) matched else activeAccs
                        val options = candidates.take(5).mapIndexed { idx, acc ->
                            VoiceDisambiguationOption(idx + 1, acc.name, acc.accountType.displayName, acc.id)
                        }
                        val q = "Which account would you like to delete or archive: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Account to Archive",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                repository.archiveAccount(chosen.entityId, true)
                                lastUndoableVoiceAction = UndoableVoiceAction.ArchivedAccount(chosen.entityId, chosen.label)
                                "Archived account ${chosen.label}. Say Undo to restore it."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    }
                }

                "ADD_SUBSCRIPTION" -> {
                    val upperHint = result.frequencyOrTypeHint.uppercase(java.util.Locale.US)
                    val freq = when (upperHint) {
                        "BIMONTHLY" -> BillingFrequency.BIMONTHLY
                        "FOUR_MONTHLY" -> BillingFrequency.FOUR_MONTHLY
                        "QUARTERLY" -> BillingFrequency.QUARTERLY
                        "HALF_YEARLY" -> BillingFrequency.HALF_YEARLY
                        "YEARLY", "ANNUALLY" -> BillingFrequency.ANNUALLY
                        "WEEKLY", "CUSTOM" -> BillingFrequency.CUSTOM
                        else -> BillingFrequency.MONTHLY
                    }
                    val customUnit = if (upperHint == "WEEKLY") "DAYS" else "MONTHS"
                    val customCount = if (upperHint == "WEEKLY") 7 else result.tenureOrIntervalCount.coerceAtLeast(1)
                    val subName = result.entityName.ifBlank { "Subscription" }
                    val cost = result.primaryAmountPaise.coerceAtLeast(10000L)
                    val today = IndianFinancialYear.todayEpochDay()
                    val nextRen = IndianFinancialYear.advanceSubscriptionRenewalEpochDay(
                        fromEpochDay = today,
                        frequency = freq,
                        customIntervalUnit = customUnit,
                        customIntervalCount = customCount
                    )
                    val defaultAccId = currentState.accountsWithBalances.firstOrNull { !it.account.isArchived }?.account?.id
                    repository.addSubscription(
                        name = subName,
                        costPaise = cost,
                        billingFrequency = freq,
                        nextRenewalEpochDay = nextRen,
                        paymentAccountId = defaultAccId,
                        notes = "Added via Voice Command",
                        isDemo = isDemo,
                        customIntervalUnit = customUnit,
                        customIntervalCount = customCount
                    )
                }

                "PAY_SUBSCRIPTION" -> {
                    val subs = currentState.subscriptions
                    val matched = subs.filter {
                        result.entityName.isNotBlank() && it.name.contains(result.entityName, ignoreCase = true)
                    }
                    val defaultAcc = repository.resolveOrCreateAccountForVoice(result.accountHint, isDemo)
                    if (matched.size == 1) {
                        val sub = matched.first()
                        repository.recordSubscriptionPaymentNow(sub.id, sub.paymentAccountId ?: defaultAcc.id)
                        finalSpokenReply = "Recorded payment of ${InrMoney.formatPaise(sub.costPaise)} for ${sub.name} and advanced its renewal date."
                    } else if (subs.size == 1 && result.entityName.isBlank()) {
                        val sub = subs.first()
                        repository.recordSubscriptionPaymentNow(sub.id, sub.paymentAccountId ?: defaultAcc.id)
                        finalSpokenReply = "Recorded payment of ${InrMoney.formatPaise(sub.costPaise)} for ${sub.name}."
                    } else if (subs.isNotEmpty()) {
                        val candidates = if (matched.isNotEmpty()) matched else subs
                        val options = candidates.take(5).mapIndexed { idx, sub ->
                            VoiceDisambiguationOption(
                                index = idx + 1,
                                label = sub.name,
                                subtitle = "${InrMoney.formatPaise(sub.costPaise)} • ${sub.cycleLabel()}",
                                entityId = sub.id
                            )
                        }
                        val q = "Which subscription would you like to pay: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Subscription to Pay",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                val sub = currentState.subscriptions.firstOrNull { it.id == chosen.entityId }
                                repository.recordSubscriptionPaymentNow(chosen.entityId, sub?.paymentAccountId ?: defaultAcc.id)
                                "Recorded subscription payment for ${chosen.label}."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    } else {
                        finalSpokenReply = "You have no active subscriptions to pay."
                    }
                }

                "DELETE_SUBSCRIPTION" -> {
                    val subs = currentState.subscriptions
                    val matched = subs.filter {
                        result.entityName.isNotBlank() && it.name.contains(result.entityName, ignoreCase = true)
                    }
                    if (subs.isEmpty()) {
                        finalSpokenReply = "You have no active subscriptions to delete."
                    } else if (matched.size == 1 || (subs.size == 1 && result.entityName.isBlank())) {
                        val sub = matched.firstOrNull() ?: subs.first()
                        repository.deleteSubscription(sub.id)
                        lastUndoableVoiceAction = UndoableVoiceAction.DeletedSubscription(sub, sub.name)
                        finalSpokenReply = "Deleted ${sub.name} subscription. Say Undo to restore it."
                    } else {
                        val candidates = if (matched.isNotEmpty()) matched else subs
                        val options = candidates.take(5).mapIndexed { idx, sub ->
                            VoiceDisambiguationOption(idx + 1, sub.name, InrMoney.formatPaise(sub.costPaise), sub.id)
                        }
                        val q = "Which subscription should I delete: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Subscription to Delete",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                val sub = currentState.subscriptions.firstOrNull { it.id == chosen.entityId }
                                if (sub != null) {
                                    lastUndoableVoiceAction = UndoableVoiceAction.DeletedSubscription(sub, sub.name)
                                }
                                repository.deleteSubscription(chosen.entityId)
                                "Deleted ${chosen.label} subscription. Say Undo to restore it."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    }
                }

                "ADD_INVESTMENT" -> {
                    val invCat = when (result.frequencyOrTypeHint.uppercase(java.util.Locale.US)) {
                        "EQUITY_SHARES", "STOCK", "EQUITY" -> InvestmentCategory.EQUITY_SHARES
                        "ETF" -> InvestmentCategory.ETF
                        "SIP" -> InvestmentCategory.SIP
                        "FIXED_DEPOSIT", "FD" -> InvestmentCategory.FIXED_DEPOSIT
                        "RECURRING_DEPOSIT", "RD" -> InvestmentCategory.RECURRING_DEPOSIT
                        "PPF", "NPS", "GOVT_SECURITIES" -> InvestmentCategory.GOVT_SECURITIES
                        "GOLD" -> InvestmentCategory.GOLD
                        "SILVER" -> InvestmentCategory.SILVER
                        "BONDS" -> InvestmentCategory.BONDS
                        "REAL_ESTATE", "OTHER" -> InvestmentCategory.OTHER
                        else -> InvestmentCategory.MUTUAL_FUND
                    }
                    val amt = result.primaryAmountPaise.coerceAtLeast(10000L)
                    val payAcc = repository.resolveOrCreateAccountForVoice(result.accountHint, isDemo)
                    repository.addInvestment(
                        name = result.entityName.ifBlank { "Portfolio Investment" },
                        category = invCat,
                        purchaseDateEpochDay = IndianFinancialYear.todayEpochDay(),
                        quantity = 1.0,
                        units = 1.0,
                        purchasePricePerUnitPaise = amt,
                        investedAmountPaise = amt,
                        chargesPaise = 0L,
                        currentRecordedValuePaise = amt,
                        isCurrentValueRecorded = true,
                        paymentAccountId = payAcc.id,
                        maturityDateEpochDay = null,
                        annualInterestRatePercent = result.ratePercent,
                        compoundingFrequency = CompoundingFrequency.ANNUALLY,
                        durationMonths = result.tenureOrIntervalCount.coerceAtLeast(12),
                        notes = "Added via Voice Command",
                        isDemo = isDemo
                    )
                }

                "ADD_LOAN" -> {
                    val lType = when (result.frequencyOrTypeHint.uppercase(java.util.Locale.US)) {
                        "HOME" -> LoanType.HOME
                        "VEHICLE", "CAR" -> LoanType.VEHICLE
                        "EDUCATION" -> LoanType.EDUCATION
                        "GOLD", "CREDIT_CARD", "OTHER" -> LoanType.OTHER
                        else -> LoanType.PERSONAL
                    }
                    val prin = result.primaryAmountPaise.coerceAtLeast(100000L)
                    val acc = repository.resolveOrCreateAccountForVoice(result.accountHint, isDemo)
                    repository.addLoan(
                        name = result.entityName.ifBlank { lType.displayName },
                        loanType = lType,
                        lenderName = result.entityName.ifBlank { "Bank Lender" },
                        principalPaise = prin,
                        annualInterestRatePercent = result.ratePercent.coerceAtLeast(1.0),
                        tenureMonths = result.tenureOrIntervalCount.coerceAtLeast(12),
                        startDateEpochDay = IndianFinancialYear.todayEpochDay(),
                        disbursedToAccountId = acc.id,
                        notes = "Added via Voice Command",
                        isDemo = isDemo
                    )
                }

                "PAY_LOAN_EMI" -> {
                    val activeLoans = currentState.loans.filter { !it.isClosed }
                    val matched = activeLoans.filter {
                        result.entityName.isNotBlank() && it.name.contains(result.entityName, ignoreCase = true)
                    }
                    val srcAcc = repository.resolveOrCreateAccountForVoice(result.accountHint, isDemo)
                    if (matched.size == 1 || (activeLoans.size == 1 && result.entityName.isBlank())) {
                        val loan = matched.firstOrNull() ?: activeLoans.first()
                        repository.recordTransaction(
                            transactionType = TransactionType.LOAN_REPAYMENT,
                            dateEpochDay = IndianFinancialYear.todayEpochDay(),
                            amountPaise = loan.emiAmountPaise,
                            principalComponentPaise = loan.emiAmountPaise,
                            interestComponentPaise = 0L,
                            sourceAccountId = srcAcc.id,
                            category = "Loan EMI Repayment",
                            description = "EMI Repayment for ${loan.name}",
                            linkedLoanId = loan.id,
                            isDemo = isDemo
                        )
                        finalSpokenReply = "Recorded EMI payment of ${InrMoney.formatPaise(loan.emiAmountPaise)} for ${loan.name}."
                    } else if (activeLoans.size > 1) {
                        val options = activeLoans.take(5).mapIndexed { idx, l ->
                            VoiceDisambiguationOption(
                                index = idx + 1,
                                label = l.name,
                                subtitle = "EMI: ${InrMoney.formatPaise(l.emiAmountPaise)}",
                                entityId = l.id
                            )
                        }
                        val q = "Which loan EMI would you like to pay: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Loan EMI to Pay",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                val loan = currentState.loans.first { it.id == chosen.entityId }
                                repository.recordTransaction(
                                    transactionType = TransactionType.LOAN_REPAYMENT,
                                    dateEpochDay = IndianFinancialYear.todayEpochDay(),
                                    amountPaise = loan.emiAmountPaise,
                                    principalComponentPaise = loan.emiAmountPaise,
                                    interestComponentPaise = 0L,
                                    sourceAccountId = srcAcc.id,
                                    category = "Loan EMI Repayment",
                                    description = "EMI Repayment for ${loan.name}",
                                    linkedLoanId = loan.id,
                                    isDemo = isDemo
                                )
                                "Recorded EMI payment of ${InrMoney.formatPaise(loan.emiAmountPaise)} for ${loan.name}."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    } else {
                        finalSpokenReply = "You have no active loans for EMI repayment."
                    }
                }

                "ADD_BUDGET" -> {
                    val cat = result.categoryOrScope.ifBlank { "OVERALL" }
                    val limit = result.primaryAmountPaise.coerceAtLeast(10000L)
                    val budgetId = repository.addBudget(
                        name = result.entityName.ifBlank { if (cat == "OVERALL") "Overall Monthly Budget" else "$cat Budget" },
                        budgetPeriod = BudgetPeriod.MONTHLY,
                        category = cat,
                        limitPaise = limit,
                        financialYear = currentState.settings.activeFinancialYear,
                        notes = "Added via Voice Command",
                        isDemo = isDemo
                    )
                    lastUndoableVoiceAction = UndoableVoiceAction.CreatedBudget(budgetId, "$cat Budget")
                }

                "DELETE_BUDGET" -> {
                    val budgets = currentState.budgets
                    val matched = budgets.filter {
                        result.entityName.isNotBlank() && (
                            it.name.contains(result.entityName, ignoreCase = true) ||
                                it.category.contains(result.entityName, ignoreCase = true)
                            )
                    }
                    if (budgets.isEmpty()) {
                        finalSpokenReply = "You have no active budgets to delete."
                    } else if (matched.size == 1 || (budgets.size == 1 && result.entityName.isBlank())) {
                        val b = matched.firstOrNull() ?: budgets.first()
                        repository.deleteBudget(b.id)
                        lastUndoableVoiceAction = UndoableVoiceAction.DeletedBudget(b, b.name)
                        finalSpokenReply = "Deleted ${b.name}. Say Undo to restore it."
                    } else {
                        val candidates = if (matched.isNotEmpty()) matched else budgets
                        val options = candidates.take(5).mapIndexed { idx, b ->
                            VoiceDisambiguationOption(idx + 1, b.name, InrMoney.formatPaise(b.limitPaise), b.id)
                        }
                        val q = "Which budget should I delete: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Budget to Delete",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                val b = currentState.budgets.firstOrNull { it.id == chosen.entityId }
                                if (b != null) {
                                    lastUndoableVoiceAction = UndoableVoiceAction.DeletedBudget(b, b.name)
                                }
                                repository.deleteBudget(chosen.entityId)
                                "Deleted ${chosen.label}. Say Undo to restore it."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    }
                }

                "ADD_GOAL" -> {
                    val target = result.primaryAmountPaise.coerceAtLeast(10000L)
                    val goalName = result.entityName.ifBlank { "Savings Goal" }
                    val goalId = repository.addGoal(
                        name = goalName,
                        goalType = GoalType.SAVINGS,
                        targetAmountPaise = target,
                        currentSavedPaise = result.secondaryAmountPaise,
                        notes = "Added via Voice Command",
                        isDemo = isDemo
                    )
                    lastUndoableVoiceAction = UndoableVoiceAction.CreatedGoal(goalId, goalName)
                }

                "DELETE_GOAL" -> {
                    val goals = currentState.goals
                    val matched = goals.filter {
                        result.entityName.isNotBlank() && it.name.contains(result.entityName, ignoreCase = true)
                    }
                    if (goals.isEmpty()) {
                        finalSpokenReply = "You have no savings goals to delete."
                    } else if (matched.size == 1 || (goals.size == 1 && result.entityName.isBlank())) {
                        val g = matched.firstOrNull() ?: goals.first()
                        repository.deleteGoal(g.id)
                        lastUndoableVoiceAction = UndoableVoiceAction.DeletedGoal(g, g.name)
                        finalSpokenReply = "Deleted savings goal ${g.name}. Say Undo to restore it."
                    } else {
                        val candidates = if (matched.isNotEmpty()) matched else goals
                        val options = candidates.take(5).mapIndexed { idx, g ->
                            VoiceDisambiguationOption(
                                idx + 1,
                                g.name,
                                "${InrMoney.formatPaise(g.currentSavedPaise)} / ${InrMoney.formatPaise(g.targetAmountPaise)}",
                                g.id
                            )
                        }
                        val q = "Which savings goal should I delete: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Savings Goal to Delete",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                val g = currentState.goals.firstOrNull { it.id == chosen.entityId }
                                if (g != null) {
                                    lastUndoableVoiceAction = UndoableVoiceAction.DeletedGoal(g, g.name)
                                }
                                repository.deleteGoal(chosen.entityId)
                                "Deleted savings goal ${chosen.label}. Say Undo to restore it."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    }
                }

                "CONTRIBUTE_GOAL" -> {
                    val goals = currentState.goals
                    val matched = goals.filter {
                        result.entityName.isNotBlank() && it.name.contains(result.entityName, ignoreCase = true)
                    }
                    val amt = result.primaryAmountPaise.coerceAtLeast(1000L)
                    if (matched.size == 1 || (goals.size == 1 && result.entityName.isBlank())) {
                        val g = matched.firstOrNull() ?: goals.first()
                        repository.contributeToGoal(g.id, amt)
                        finalSpokenReply = "Added ${InrMoney.formatPaise(amt)} towards your ${g.name} goal."
                    } else if (goals.size > 1) {
                        val options = goals.take(5).mapIndexed { idx, g ->
                            VoiceDisambiguationOption(
                                index = idx + 1,
                                label = g.name,
                                subtitle = "${InrMoney.formatPaise(g.currentSavedPaise)} / ${InrMoney.formatPaise(g.targetAmountPaise)}",
                                entityId = g.id
                            )
                        }
                        val q = "Which savings goal should I add ${InrMoney.formatPaise(amt)} to: ${options.joinToString(", or ") { "${it.index}. ${it.label}" }}?"
                        _pendingVoiceDisambiguation.value = PendingVoiceDisambiguation(
                            title = "Select Goal for ${InrMoney.formatPaise(amt)}",
                            questionPrompt = q,
                            options = options,
                            onSelectOption = { chosen ->
                                repository.contributeToGoal(chosen.entityId, amt)
                                "Added ${InrMoney.formatPaise(amt)} towards ${chosen.label}."
                            }
                        )
                        finalSpokenReply = q
                        listenForFollowUp = true
                    } else {
                        finalSpokenReply = "You have no savings goals yet. Say Create a savings goal first."
                    }
                }

                "ADD_GIFT_CARD" -> {
                    val valPaise = result.primaryAmountPaise.coerceAtLeast(10000L)
                    val brand = result.entityName.ifBlank { "Gift Card" }
                    repository.addGiftCard(
                        brand = brand,
                        name = "$brand Voucher",
                        initialValuePaise = valPaise,
                        remainingValuePaise = valPaise,
                        purchaseDateEpochDay = IndianFinancialYear.todayEpochDay(),
                        cardNumberRaw = "XXXX-1001",
                        redemptionPinRaw = "0000",
                        purchasedFromAccountId = null,
                        notes = "Added via Voice Command",
                        isDemo = isDemo
                    )
                }

                "ADD_TAX_RECORD" -> {
                    val amt = result.primaryAmountPaise.coerceAtLeast(10000L)
                    val acc = repository.resolveOrCreateAccountForVoice(result.accountHint, isDemo)
                    repository.addTaxRecord(
                        taxType = TaxType.ADVANCE_TAX,
                        financialYear = currentState.settings.activeFinancialYear,
                        amountPaidPaise = amt,
                        paymentDateEpochDay = IndianFinancialYear.todayEpochDay(),
                        challanNumber = result.entityName.ifBlank { "VOICE-TAX" },
                        paymentAccountId = acc.id,
                        supportingDocLabel = "Voice Entry",
                        notes = "Added via Voice Command",
                        isDemo = isDemo
                    )
                }

                "SEARCH" -> {
                    val q = result.searchQuery.trim()
                    if (q.isNotBlank()) {
                        updateSearchQuery(q)
                        commitSearchQuery(q)
                        collapseSearchBar()
                    }
                }

                "CLEAR_SEARCH" -> {
                    closeAndResetSearch()
                }

                "EXPORT_REPORT" -> {
                    when (result.exportFormat.uppercase(java.util.Locale.US)) {
                        "PDF" -> preparePdfExport(context)
                        "EXCEL" -> prepareCsvOrExcelExport(context, asExcel = true)
                        "CSV" -> prepareCsvOrExcelExport(context, asExcel = false)
                        "JSON" -> prepareJsonBackupExport(context)
                        else -> preparePdfExport(context)
                    }
                }

                "UPDATE_SETTING" -> {
                    when (result.settingKey.uppercase(java.util.Locale.US)) {
                        "PRIVACY_MODE" -> {
                            val enable = result.settingValue.equals("ON", ignoreCase = true)
                            repository.togglePrivacyMode(enable)
                        }
                        "THEME_MODE" -> {
                            val mode = when (result.settingValue.uppercase(java.util.Locale.US)) {
                                "LIGHT" -> ThemeMode.LIGHT
                                "SYSTEM" -> ThemeMode.SYSTEM
                                else -> ThemeMode.DARK
                            }
                            repository.updateThemeMode(mode)
                        }
                        "DEMO_MODE" -> {
                            val enable = result.settingValue.equals("ON", ignoreCase = true)
                            repository.setDemonstrationMode(enable)
                        }
                        "LOCK_APP" -> {
                            lockAppNow()
                        }
                    }
                }
            }

                val updatedState = uiState.value
                val totalBalPaise = updatedState.accountsWithBalances
                    .filter { !it.account.isArchived }
                    .sumOf { it.closingBalancePaise }
                val payload = AssistantFulfillmentPayload(
                    spokenReply = finalSpokenReply,
                    netWorthFormatted = InrMoney.formatPaise(updatedState.balanceSheet.totalNetWorthPaise),
                    totalBalanceFormatted = InrMoney.formatPaise(totalBalPaise),
                    monthlySpendingFormatted = InrMoney.formatPaise(updatedState.currentMonthTotalSpentPaise),
                    monthlyIncomeFormatted = InrMoney.formatPaise(updatedState.monthlyProfitAndLoss.totalIncomePaise),
                    recordedTransactionsCount = if (result.action == "RECORD_TRANSACTION" && !listenForFollowUp) result.transactionsToRecord.size else 0,
                    errorMessage = result.errorMessage
                )

                _statusBannerMessage.value = finalSpokenReply
                onAssistantFulfillment(payload)
                onFinished(finalSpokenReply, result.errorMessage, listenForFollowUp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val fallbackMsg = "Could not complete voice command: ${e.localizedMessage ?: "Please try again."}"
                _statusBannerMessage.value = fallbackMsg
                onFinished(fallbackMsg, e.localizedMessage, false)
            }
        }
    }

    /**
     * Handles incoming intents from Google Assistant, Google Gemini App Actions,
     * Android Voice Shortcuts, and 6-Hour / Custom Transaction Recall Notifications,
     * returning relevant financial information back to Google Assistant and the user.
     */
    fun handleExternalGeminiOrVoiceIntent(
        context: Context,
        intent: Intent?,
        onStartVoiceListening: () -> Unit,
        onFinishedReply: (String, String?, Boolean) -> Unit,
        onAssistantFulfillment: (AssistantFulfillmentPayload) -> Unit = {}
    ) {
        if (intent == null) return

        if (intent.getBooleanExtra(TransactionRecallReminderManager.EXTRA_OPEN_RECORD_SHEET, false)) {
            intent.removeExtra(TransactionRecallReminderManager.EXTRA_OPEN_RECORD_SHEET)
            openAddTransactionSheet(true)
            return
        }

        if (intent.getBooleanExtra(TransactionRecallReminderManager.EXTRA_START_VOICE_RECORD, false)) {
            intent.removeExtra(TransactionRecallReminderManager.EXTRA_START_VOICE_RECORD)
            onStartVoiceListening()
            return
        }

        val dataUri = intent.data
        if (dataUri != null && dataUri.scheme.equals("vault", ignoreCase = true)) {
            val host = dataUri.host?.lowercase().orEmpty()
            val queryParam = dataUri.getQueryParameter("query").orEmpty()
            val featureParam = dataUri.getQueryParameter("feature").orEmpty()
            val amountParam = dataUri.getQueryParameter("amount").orEmpty()
            val categoryParam = dataUri.getQueryParameter("category").orEmpty()
            val merchantParam = dataUri.getQueryParameter("merchant").orEmpty()
            val typeParam = dataUri.getQueryParameter("type").orEmpty()
            val accountParam = dataUri.getQueryParameter("account").orEmpty()
            intent.data = null

            when {
                host == "voice-record" || featureParam.contains("voice", ignoreCase = true) -> {
                    if (queryParam.isNotBlank()) {
                        executeVoiceOrGeminiCommand(
                            context = context,
                            utterance = queryParam,
                            onFinished = onFinishedReply,
                            onAssistantFulfillment = onAssistantFulfillment
                        )
                    } else {
                        onStartVoiceListening()
                    }
                }

                host == "record-transaction" || amountParam.isNotBlank() -> {
                    val constructedSentence = if (queryParam.isNotBlank()) {
                        queryParam
                    } else if (amountParam.isNotBlank()) {
                        val actionWord = if (typeParam.equals("INCOME", ignoreCase = true)) "Received" else "Spent"
                        val catPart = if (categoryParam.isNotBlank()) " on $categoryParam" else ""
                        val merchPart = if (merchantParam.isNotBlank()) " at $merchantParam" else ""
                        val accPart = if (accountParam.isNotBlank()) " from $accountParam" else ""
                        "$actionWord $amountParam rupees$catPart$merchPart$accPart"
                    } else ""

                    if (constructedSentence.isNotBlank()) {
                        executeVoiceOrGeminiCommand(
                            context = context,
                            utterance = constructedSentence,
                            onFinished = onFinishedReply,
                            onAssistantFulfillment = onAssistantFulfillment
                        )
                    } else {
                        onStartVoiceListening()
                    }
                }

                host == "query-finance" || host == "assistant" -> {
                    val q = queryParam.ifBlank {
                        when {
                            accountParam.isNotBlank() -> "What is my $accountParam balance?"
                            featureParam.isNotBlank() -> "Tell me about my $featureParam"
                            else -> "Give me a summary of my balances, net worth, and monthly spending"
                        }
                    }
                    _showGeminiAssistantDialog.value = true
                    executeVoiceOrGeminiCommand(
                        context = context,
                        utterance = q,
                        onFinished = onFinishedReply,
                        onAssistantFulfillment = onAssistantFulfillment
                    )
                }
            }
            return
        }

        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
            if (sharedText.isNotBlank()) {
                intent.removeExtra(Intent.EXTRA_TEXT)
                executeVoiceOrGeminiCommand(
                    context = context,
                    utterance = sharedText,
                    onFinished = onFinishedReply,
                    onAssistantFulfillment = onAssistantFulfillment
                )
            }
            return
        }

        if (
            intent.action == "com.google.android.gms.actions.SEARCH_ACTION" ||
            intent.action == Intent.ACTION_VOICE_COMMAND ||
            intent.action == Intent.ACTION_ASSIST
        ) {
            val voiceQuery = intent.getStringExtra(android.app.SearchManager.QUERY)
                ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra("query")
                ?: ""
            intent.removeExtra(android.app.SearchManager.QUERY)
            val trimmed = voiceQuery.trim()
            if (trimmed.isNotBlank()) {
                _showGeminiAssistantDialog.value = true
                executeVoiceOrGeminiCommand(
                    context = context,
                    utterance = trimmed,
                    onFinished = onFinishedReply,
                    onAssistantFulfillment = onAssistantFulfillment
                )
            } else {
                // If Google Assistant launched VOICE_COMMAND or ASSIST without a query string, return summary & start listening
                val summaryReply = GeminiVaultAssistant.answerFinancialQueryLocally("summary", uiState.value)
                val s = uiState.value
                val totalBalPaise = s.accountsWithBalances
                    .filter { !it.account.isArchived }
                    .sumOf { it.closingBalancePaise }
                onAssistantFulfillment(
                    AssistantFulfillmentPayload(
                        spokenReply = summaryReply,
                        netWorthFormatted = InrMoney.formatPaise(s.balanceSheet.totalNetWorthPaise),
                        totalBalanceFormatted = InrMoney.formatPaise(totalBalPaise),
                        monthlySpendingFormatted = InrMoney.formatPaise(s.currentMonthTotalSpentPaise),
                        monthlyIncomeFormatted = InrMoney.formatPaise(s.monthlyProfitAndLoss.totalIncomePaise),
                        recordedTransactionsCount = 0
                    )
                )
                onStartVoiceListening()
            }
        }
    }

    fun addTaxRecord(
        taxType: TaxType,
        financialYear: String,
        amountPaidPaise: Long,
        challanNumber: String,
        paymentAccountId: Long?,
        supportingDocLabel: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.addTaxRecord(
                    taxType = taxType,
                    financialYear = financialYear,
                    amountPaidPaise = amountPaidPaise,
                    paymentDateEpochDay = IndianFinancialYear.todayEpochDay(),
                    challanNumber = challanNumber,
                    paymentAccountId = paymentAccountId,
                    supportingDocLabel = supportingDocLabel,
                    notes = notes,
                    isDemo = uiState.value.settings.isDemoModeEnabled
                )
            }.onSuccess {
                _statusBannerMessage.value = "Tax record saved for $financialYear."
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not save tax record: ${e.message}"
            }
        }
    }

    fun updateProfileAndFy(userName: String, panMasked: String, activeFy: String = IndianFinancialYear.currentFyLabel()) {
        viewModelScope.launch(Dispatchers.IO) {
            val systemFy = IndianFinancialYear.currentFyLabel()
            repository.updateProfileAndFy(userName, panMasked, systemFy)
            _statusBannerMessage.value = "Profile updated ($systemFy)."
        }
    }

    fun updateSecurityPin(newPin: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSecurityPin(newPin)
            _isPinUnlockedSession.value = true
            _statusBannerMessage.value = if (newPin.isNullOrBlank()) {
                "PIN & Biometric lock removed."
            } else {
                "PIN lock enabled (SHA-256 salted hash)."
            }
        }
    }

    fun updateThemeMode(themeMode: com.example.data.local.ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateThemeMode(themeMode)
            _statusBannerMessage.value = "Appearance updated to ${themeMode.displayName}."
        }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled && uiState.value.settings.pinHashSha256.isNullOrBlank()) {
                _statusBannerMessage.value = "Please set a 4–8 digit backup PIN first before enabling Biometrics."
                return@launch
            }
            repository.setBiometricEnabled(enabled)
            _statusBannerMessage.value = if (enabled) {
                "Biometric Unlock (Fingerprint / Face ID) enabled."
            } else {
                "Biometric Unlock disabled (PIN lock remains active)."
            }
        }
    }

    fun unlockWithBiometrics() {
        _isPinUnlockedSession.value = true
    }

    fun clearCurrentModeData() {
        viewModelScope.launch(Dispatchers.IO) {
            val isDemo = uiState.value.settings.isDemoModeEnabled
            repository.clearAllDataForMode(isDemo)
            _statusBannerMessage.value = if (isDemo) {
                "Demonstration records reset."
            } else {
                "All user financial records permanently deleted."
            }
        }
    }

    fun preparePdfExport(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val s = uiState.value
                val preview = DocumentExporter.buildFullReportPreviewText(
                    userName = s.settings.userName,
                    financialYear = s.settings.activeFinancialYear,
                    periodLabel = s.settings.activeFinancialYear,
                    generationDate = IndianFinancialYear.formatEpochDay(IndianFinancialYear.todayEpochDay()),
                    balanceSheet = s.balanceSheet,
                    profitAndLoss = s.fyProfitAndLoss,
                    cashFlow = s.fyCashFlow,
                    trialBalance = s.trialBalance,
                    journalBook = s.journalBook
                )
                DocumentExporter.exportFinancialReportPdf(
                    context = context,
                    title = "Finance Vault Financial Statements (${s.settings.activeFinancialYear})",
                    reportText = preview
                )
            }.onSuccess { res ->
                _exportPreviewResult.value = res
                _statusBannerMessage.value = "Downloaded PDF: ${res.downloadedLocationLabel}"
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not export PDF: ${e.message}"
            }
        }
    }

    fun prepareCsvOrExcelExport(context: Context, asExcel: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val s = uiState.value
                DocumentExporter.exportFinancialReportCsv(
                    context = context,
                    financialYear = s.settings.activeFinancialYear,
                    balanceSheet = s.balanceSheet,
                    profitAndLoss = s.fyProfitAndLoss,
                    trialBalance = s.trialBalance,
                    journalBook = s.journalBook,
                    asExcelExtension = asExcel
                )
            }.onSuccess { res ->
                _exportPreviewResult.value = res
                _statusBannerMessage.value = if (asExcel) {
                    "Downloaded Excel (.xlsx): ${res.downloadedLocationLabel}"
                } else {
                    "Downloaded CSV (.csv): ${res.downloadedLocationLabel}"
                }
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not export spreadsheet: ${e.message}"
            }
        }
    }

    fun prepareJsonBackupExport(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = repository.createJsonBackupString()
                DocumentExporter.exportJsonBackupFile(context, json)
            }.onSuccess { res ->
                _exportPreviewResult.value = res
                _statusBannerMessage.value = "Downloaded JSON Backup: ${res.downloadedLocationLabel}"
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not export JSON backup: ${e.message}"
            }
        }
    }

    fun restoreFromJsonBackup(jsonText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.restoreFromJsonBackup(jsonText)
            }.onSuccess { count ->
                _statusBannerMessage.value = "Restored $count account(s) from JSON backup."
            }.onFailure { e ->
                _statusBannerMessage.value = "Restore failed: ${e.message}"
            }
        }
    }

    private data class CoreAccountingBundle(
        val accounts: List<AccountEntity>,
        val ledgers: List<LedgerAccountEntity>,
        val txs: List<TransactionEntity>,
        val entries: List<JournalEntryEntity>,
        val lines: List<JournalLineEntity>
    )

    private data class SecondaryBundle1(
        val investments: List<InvestmentEntity>,
        val loans: List<LoanEntity>,
        val loanPayments: List<LoanPaymentEntity>,
        val budgets: List<BudgetEntity>,
        val goals: List<GoalEntity>
    )

    private data class SecondaryBundle2(
        val giftCards: List<GiftCardEntity>,
        val subscriptions: List<SubscriptionEntity>,
        val taxes: List<TaxRecordEntity>,
        val logs: List<AuditLogEntity>
    )

    class Factory(private val repository: FinanceVaultRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinanceVaultViewModel(repository) as T
        }
    }
}
