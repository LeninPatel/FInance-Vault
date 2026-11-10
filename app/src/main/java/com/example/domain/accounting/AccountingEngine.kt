package com.example.domain.accounting

import com.example.data.local.AccountType
import com.example.data.local.JournalLineEntity
import com.example.data.local.LedgerAccountEntity
import com.example.data.local.LedgerClassification
import com.example.data.local.NormalBalance
import com.example.data.local.TransactionType

/**
 * Pure double-entry bookkeeping engine for Finance Vault.
 * Validates transactions, resolves affected accounts, generates debit and credit lines,
 * and enforces the fundamental accounting invariant: Total Debits == Total Credits.
 */
object AccountingEngine {

    data class DraftJournalLine(
        val ledgerAccount: LedgerAccountEntity,
        val debitPaise: Long,
        val creditPaise: Long,
        val narration: String
    ) {
        init {
            require(debitPaise >= 0L && creditPaise >= 0L) {
                "Debit and credit amounts must be non-negative."
            }
            require((debitPaise > 0L && creditPaise == 0L) || (creditPaise > 0L && debitPaise == 0L)) {
                "Each journal line must have either a positive debit or a positive credit, not both or zero."
            }
        }
    }

    data class ValidatedPostingPlan(
        val narration: String,
        val lines: List<DraftJournalLine>,
        val totalDebitPaise: Long,
        val totalCreditPaise: Long
    ) {
        init {
            require(lines.size >= 2) { "A double-entry journal entry requires at least two lines." }
            require(totalDebitPaise > 0L) { "Journal entry total must be greater than 0 paise." }
            require(totalDebitPaise == totalCreditPaise) {
                "Double-entry invariant violated: Total Debits ($totalDebitPaise) != Total Credits ($totalCreditPaise)"
            }
        }
    }

    /**
     * Maps a user-friendly income category string to the appropriate Indian P&L Income classification.
     */
    fun resolveIncomeClassification(category: String): LedgerClassification {
        val lower = category.trim().lowercase()
        return when {
            lower.contains("salary") || lower.contains("wage") || lower.contains("stipend") || lower.contains("bonus") ->
                LedgerClassification.INCOME_SALARY
            lower.contains("business") || lower.contains("freelance") || lower.contains("consulting") || lower.contains("profession") ->
                LedgerClassification.INCOME_BUSINESS
            lower.contains("interest") ->
                LedgerClassification.INCOME_INTEREST
            lower.contains("dividend") ->
                LedgerClassification.INCOME_DIVIDEND
            lower.contains("capital gain") || lower.contains("profit on sale") ->
                LedgerClassification.INCOME_CAPITAL_GAIN
            else ->
                LedgerClassification.INCOME_OTHER
        }
    }

    /**
     * Maps a user-friendly expense category string to the appropriate Indian P&L Expense classification.
     */
    fun resolveExpenseClassification(category: String): LedgerClassification {
        val lower = category.trim().lowercase()
        return when {
            lower.contains("house") || lower.contains("grocery") || lower.contains("groceries") ||
                lower.contains("food") || lower.contains("rent") || lower.contains("dining") ||
                lower.contains("home") || lower.contains("maintenance") ->
                LedgerClassification.EXPENSE_HOUSEHOLD
            lower.contains("educat") || lower.contains("tuition") || lower.contains("school") ||
                lower.contains("college") || lower.contains("book") || lower.contains("course") ->
                LedgerClassification.EXPENSE_EDUCATION
            lower.contains("transport") || lower.contains("fuel") || lower.contains("petrol") ||
                lower.contains("travel") || lower.contains("cab") || lower.contains("metro") ||
                lower.contains("auto") || lower.contains("flight") || lower.contains("train") ->
                LedgerClassification.EXPENSE_TRANSPORT
            lower.contains("utilit") || lower.contains("electric") || lower.contains("water") ||
                lower.contains("internet") || lower.contains("broadband") || lower.contains("mobile") ||
                lower.contains("recharge") || lower.contains("gas") || lower.contains("subscription") ->
                LedgerClassification.EXPENSE_UTILITIES
            lower.contains("health") || lower.contains("medical") || lower.contains("hospital") ||
                lower.contains("pharmacy") || lower.contains("medicine") || lower.contains("doctor") ||
                lower.contains("insurance") ->
                LedgerClassification.EXPENSE_HEALTHCARE
            lower.contains("interest") || lower.contains("emi interest") ->
                LedgerClassification.EXPENSE_INTEREST
            lower.contains("bank charge") || lower.contains("fee") || lower.contains("sms charge") ||
                lower.contains("penalty") ->
                LedgerClassification.EXPENSE_BANK_CHARGES
            lower.contains("brokerage") || lower.contains("stt") || lower.contains("investment fee") ->
                LedgerClassification.EXPENSE_INVESTMENT_FEES
            lower.contains("tax") || lower.contains("tds") || lower.contains("gst") ->
                LedgerClassification.EXPENSE_TAXES
            else ->
                LedgerClassification.EXPENSE_OTHER
        }
    }

    /**
     * Builds and validates the double-entry journal lines for any supported transaction type.
     */
    fun buildPostingPlan(
        transactionType: TransactionType,
        amountPaise: Long,
        principalComponentPaise: Long = 0L,
        interestComponentPaise: Long = 0L,
        chargesPaise: Long = 0L,
        costBasisDisposedPaise: Long = 0L,
        description: String,
        sourceLedger: LedgerAccountEntity? = null,
        destinationLedger: LedgerAccountEntity? = null,
        categoryLedger: LedgerAccountEntity? = null,
        openingCapitalLedger: LedgerAccountEntity? = null,
        interestExpenseLedger: LedgerAccountEntity? = null,
        interestIncomeLedger: LedgerAccountEntity? = null,
        investmentFeeLedger: LedgerAccountEntity? = null,
        capitalGainLedger: LedgerAccountEntity? = null,
        capitalLossLedger: LedgerAccountEntity? = null
    ): ValidatedPostingPlan {
        require(amountPaise > 0L) { "Transaction amount must be strictly greater than ₹0." }

        val lines = mutableListOf<DraftJournalLine>()
        val cleanDesc = description.ifBlank { transactionType.userLabel }

        when (transactionType) {
            TransactionType.OPENING_BALANCE -> {
                val target = requireNotNull(destinationLedger ?: sourceLedger) {
                    "Account ledger is required for opening balance."
                }
                val cap = requireNotNull(openingCapitalLedger) {
                    "Opening Capital ledger is required for opening balance."
                }
                if (target.normalBalance == NormalBalance.DEBIT) {
                    lines.add(DraftJournalLine(target, debitPaise = amountPaise, creditPaise = 0L, narration = "Opening balance for ${target.name}"))
                    lines.add(DraftJournalLine(cap, debitPaise = 0L, creditPaise = amountPaise, narration = "Opening capital from ${target.name}"))
                } else {
                    // Liability opening balance (e.g., existing Credit Card due or Loan)
                    lines.add(DraftJournalLine(cap, debitPaise = amountPaise, creditPaise = 0L, narration = "Opening liability adjustment for ${target.name}"))
                    lines.add(DraftJournalLine(target, debitPaise = 0L, creditPaise = amountPaise, narration = "Opening outstanding for ${target.name}"))
                }
            }

            TransactionType.INCOME -> {
                val dest = requireNotNull(destinationLedger) { "Destination bank/cash account is required for income." }
                val inc = requireNotNull(categoryLedger) { "Income category ledger is required." }
                lines.add(DraftJournalLine(dest, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(inc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.EXPENSE -> {
                val src = requireNotNull(sourceLedger) { "Source payment account is required for expense." }
                val exp = requireNotNull(categoryLedger) { "Expense category ledger is required." }
                lines.add(DraftJournalLine(exp, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(src, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.TRANSFER,
            TransactionType.CASH_DEPOSIT,
            TransactionType.CASH_WITHDRAWAL,
            TransactionType.CORRECTION -> {
                val src = requireNotNull(sourceLedger) { "Source account is required for transfer." }
                val dest = requireNotNull(destinationLedger) { "Destination account is required for transfer." }
                require(src.id != dest.id) { "Source and destination accounts must be different." }
                lines.add(DraftJournalLine(dest, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(src, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.CREDIT_CARD_PURCHASE -> {
                val ccLiability = requireNotNull(sourceLedger) { "Credit card account is required." }
                val exp = requireNotNull(categoryLedger) { "Expense category ledger is required." }
                lines.add(DraftJournalLine(exp, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(ccLiability, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.CREDIT_CARD_REPAYMENT -> {
                val bankSrc = requireNotNull(sourceLedger) { "Source bank/cash account is required for credit card payment." }
                val ccDest = requireNotNull(destinationLedger) { "Credit card account is required for repayment." }
                lines.add(DraftJournalLine(ccDest, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(bankSrc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.INVESTMENT_PURCHASE,
            TransactionType.RD_CONTRIBUTION -> {
                val bankSrc = requireNotNull(sourceLedger) { "Source bank/cash account is required for investment purchase." }
                val invAsset = requireNotNull(destinationLedger) { "Investment asset ledger is required." }
                if (chargesPaise > 0L) {
                    val feeLedger = requireNotNull(investmentFeeLedger) { "Investment fee ledger is required when brokerage charges exist." }
                    val totalOutflow = amountPaise + chargesPaise
                    lines.add(DraftJournalLine(invAsset, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                    lines.add(DraftJournalLine(feeLedger, debitPaise = chargesPaise, creditPaise = 0L, narration = "Brokerage / charges for $cleanDesc"))
                    lines.add(DraftJournalLine(bankSrc, debitPaise = 0L, creditPaise = totalOutflow, narration = cleanDesc))
                } else {
                    lines.add(DraftJournalLine(invAsset, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                    lines.add(DraftJournalLine(bankSrc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
                }
            }

            TransactionType.INVESTMENT_SALE -> {
                val invAsset = requireNotNull(sourceLedger) { "Investment asset ledger is required for investment sale." }
                val bankDest = requireNotNull(destinationLedger) { "Destination bank account is required for sale proceeds." }
                val costDisposed = if (costBasisDisposedPaise > 0L) costBasisDisposedPaise else amountPaise
                val netProceedsToBank = (amountPaise - chargesPaise).coerceAtLeast(1L)

                lines.add(DraftJournalLine(bankDest, debitPaise = netProceedsToBank, creditPaise = 0L, narration = "Sale proceeds: $cleanDesc"))
                if (chargesPaise > 0L) {
                    val feeLedger = requireNotNull(investmentFeeLedger) { "Investment fee ledger required." }
                    lines.add(DraftJournalLine(feeLedger, debitPaise = chargesPaise, creditPaise = 0L, narration = "Charges on sale: $cleanDesc"))
                }
                lines.add(DraftJournalLine(invAsset, debitPaise = 0L, creditPaise = costDisposed, narration = "Cost basis disposed: $cleanDesc"))

                val gainOrLoss = amountPaise - costDisposed
                if (gainOrLoss > 0L) {
                    val gainLedger = requireNotNull(capitalGainLedger) { "Capital Gain ledger required." }
                    lines.add(DraftJournalLine(gainLedger, debitPaise = 0L, creditPaise = gainOrLoss, narration = "Realized gain on $cleanDesc"))
                } else if (gainOrLoss < 0L) {
                    val lossLedger = requireNotNull(capitalLossLedger) { "Capital Loss ledger required." }
                    lines.add(DraftJournalLine(lossLedger, debitPaise = -gainOrLoss, creditPaise = 0L, narration = "Realized loss on $cleanDesc"))
                }
            }

            TransactionType.FD_MATURITY,
            TransactionType.RD_MATURITY -> {
                val invAsset = requireNotNull(sourceLedger) { "Deposit asset ledger is required." }
                val bankDest = requireNotNull(destinationLedger) { "Destination bank account is required." }
                val principal = if (principalComponentPaise > 0L) principalComponentPaise else (amountPaise - interestComponentPaise).coerceAtLeast(0L)
                val interest = (amountPaise - principal).coerceAtLeast(0L)
                lines.add(DraftJournalLine(bankDest, debitPaise = amountPaise, creditPaise = 0L, narration = "Maturity proceeds: $cleanDesc"))
                if (principal > 0L) {
                    lines.add(DraftJournalLine(invAsset, debitPaise = 0L, creditPaise = principal, narration = "Principal matured: $cleanDesc"))
                }
                if (interest > 0L) {
                    val intInc = requireNotNull(interestIncomeLedger) { "Interest Income ledger required." }
                    lines.add(DraftJournalLine(intInc, debitPaise = 0L, creditPaise = interest, narration = "Interest earned on maturity: $cleanDesc"))
                }
            }

            TransactionType.LOAN_RECEIVED -> {
                val loanLiability = requireNotNull(sourceLedger) { "Loan liability ledger is required." }
                val bankDest = requireNotNull(destinationLedger) { "Destination bank account is required for loan receipt." }
                lines.add(DraftJournalLine(bankDest, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(loanLiability, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.LOAN_REPAYMENT -> {
                val bankSrc = requireNotNull(sourceLedger) { "Source bank account is required for loan repayment." }
                val loanLiability = requireNotNull(destinationLedger) { "Loan liability ledger is required for loan repayment." }
                val principal = if (principalComponentPaise > 0L || interestComponentPaise > 0L) {
                    principalComponentPaise
                } else {
                    amountPaise
                }
                val interest = if (principalComponentPaise > 0L || interestComponentPaise > 0L) {
                    interestComponentPaise
                } else {
                    0L
                }
                require(principal + interest == amountPaise) {
                    "Principal (${principal}p) + Interest (${interest}p) must equal total repayment (${amountPaise}p)."
                }
                if (principal > 0L) {
                    lines.add(DraftJournalLine(loanLiability, debitPaise = principal, creditPaise = 0L, narration = "Loan principal repayment: $cleanDesc"))
                }
                if (interest > 0L) {
                    val intExp = requireNotNull(interestExpenseLedger) { "Interest Expense ledger is required." }
                    lines.add(DraftJournalLine(intExp, debitPaise = interest, creditPaise = 0L, narration = "Loan interest payment: $cleanDesc"))
                }
                lines.add(DraftJournalLine(bankSrc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.INTEREST_RECEIVED -> {
                val bankDest = requireNotNull(destinationLedger) { "Destination bank account is required." }
                val intInc = requireNotNull(interestIncomeLedger ?: categoryLedger) { "Interest Income ledger is required." }
                lines.add(DraftJournalLine(bankDest, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(intInc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.DIVIDEND_RECEIVED -> {
                val bankDest = requireNotNull(destinationLedger) { "Destination bank account is required." }
                val divInc = requireNotNull(categoryLedger) { "Dividend Income ledger is required." }
                lines.add(DraftJournalLine(bankDest, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(divInc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.REFUND_RECEIVED -> {
                val destAccount = requireNotNull(destinationLedger) { "Destination bank/card account is required for refund." }
                val originalExpenseLedger = requireNotNull(categoryLedger) { "Original expense category ledger is required to credit refund." }
                // Refunds reduce the expense account directly rather than inflating income
                lines.add(DraftJournalLine(destAccount, debitPaise = amountPaise, creditPaise = 0L, narration = "Refund received: $cleanDesc"))
                lines.add(DraftJournalLine(originalExpenseLedger, debitPaise = 0L, creditPaise = amountPaise, narration = "Expense reversal via refund: $cleanDesc"))
            }

            TransactionType.BANK_CHARGE,
            TransactionType.TAX_PAYMENT -> {
                val bankSrc = requireNotNull(sourceLedger) { "Source payment account is required." }
                val expLedger = requireNotNull(categoryLedger) { "Expense/Tax ledger is required." }
                lines.add(DraftJournalLine(expLedger, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(bankSrc, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.GIFT_CARD_PURCHASE -> {
                val srcAccount = requireNotNull(sourceLedger) { "Source payment account is required." }
                val giftCardAsset = requireNotNull(destinationLedger) { "Gift card asset ledger is required." }
                lines.add(DraftJournalLine(giftCardAsset, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(srcAccount, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.GIFT_CARD_USAGE -> {
                val giftCardAsset = requireNotNull(sourceLedger) { "Gift card asset ledger is required." }
                val expLedger = requireNotNull(categoryLedger) { "Expense category ledger is required." }
                lines.add(DraftJournalLine(expLedger, debitPaise = amountPaise, creditPaise = 0L, narration = cleanDesc))
                lines.add(DraftJournalLine(giftCardAsset, debitPaise = 0L, creditPaise = amountPaise, narration = cleanDesc))
            }

            TransactionType.REVERSAL -> {
                throw IllegalArgumentException("Use buildReversalPlan for reversing an existing journal entry.")
            }
        }

        val totalDebits = lines.sumOf { it.debitPaise }
        val totalCredits = lines.sumOf { it.creditPaise }

        return ValidatedPostingPlan(
            narration = cleanDesc,
            lines = lines,
            totalDebitPaise = totalDebits,
            totalCreditPaise = totalCredits
        )
    }

    /**
     * Generates a reversal posting plan that flips debits and credits of an existing journal entry.
     */
    fun buildReversalPlan(
        originalVoucherNumber: String,
        originalLines: List<JournalLineEntity>,
        ledgerById: Map<Long, LedgerAccountEntity>
    ): ValidatedPostingPlan {
        require(originalLines.isNotEmpty()) { "Cannot reverse an empty journal entry." }
        val reversedLines = originalLines.map { line ->
            val ledger = requireNotNull(ledgerById[line.ledgerAccountId]) {
                "Missing ledger account ${line.ledgerAccountId} for reversal."
            }
            DraftJournalLine(
                ledgerAccount = ledger,
                debitPaise = line.creditPaise,
                creditPaise = line.debitPaise,
                narration = "Reversal of $originalVoucherNumber: ${line.lineNarration}"
            )
        }
        return ValidatedPostingPlan(
            narration = "Reversal of Voucher $originalVoucherNumber",
            lines = reversedLines,
            totalDebitPaise = reversedLines.sumOf { it.debitPaise },
            totalCreditPaise = reversedLines.sumOf { it.creditPaise }
        )
    }

    /**
     * Calculates closing balance for a ledger account according to its normal balance:
     * - Debit-normal: Opening + Debits - Credits
     * - Credit-normal: Opening + Credits - Debits
     */
    fun calculateClosingBalance(
        normalBalance: NormalBalance,
        openingBalancePaise: Long = 0L,
        totalDebitsPaise: Long,
        totalCreditsPaise: Long
    ): Long {
        return when (normalBalance) {
            NormalBalance.DEBIT -> openingBalancePaise + totalDebitsPaise - totalCreditsPaise
            NormalBalance.CREDIT -> openingBalancePaise + totalCreditsPaise - totalDebitsPaise
        }
    }
}
