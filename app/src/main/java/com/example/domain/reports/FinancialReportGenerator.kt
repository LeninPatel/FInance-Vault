package com.example.domain.reports

import androidx.compose.runtime.Immutable
import com.example.data.local.JournalEntryEntity
import com.example.data.local.JournalLineEntity
import com.example.data.local.LedgerAccountEntity
import com.example.data.local.LedgerClassification
import com.example.data.local.NormalBalance
import com.example.domain.accounting.AccountingEngine

/**
 * Generates all Indian accounting documents and personal financial statements
 * directly from posted JournalEntry, JournalLine, and LedgerAccount records.
 * Single source of truth: no figure is hardcoded or computed independently of the ledgers.
 */
object FinancialReportGenerator {

    // 1. Journal Book
    @Immutable
    data class JournalBookLineItem(
        val dateEpochDay: Long,
        val voucherNumber: String,
        val particulars: String,
        val ledgerFolio: String,
        val debitPaise: Long,
        val creditPaise: Long,
        val isNarrationRow: Boolean = false
    )

    @Immutable
    data class JournalBookReport(
        val entriesCount: Int,
        val rows: List<JournalBookLineItem>,
        val totalDebitPaise: Long,
        val totalCreditPaise: Long,
        val isBalanced: Boolean
    )

    // 2. Individual Account Ledger
    @Immutable
    data class AccountLedgerRow(
        val dateEpochDay: Long,
        val particulars: String,
        val voucherNumber: String,
        val debitPaise: Long,
        val creditPaise: Long,
        val runningBalancePaise: Long,
        val balanceSuffix: String // "Dr" or "Cr"
    )

    @Immutable
    data class IndividualAccountLedgerReport(
        val ledgerAccount: LedgerAccountEntity,
        val openingBalancePaise: Long,
        val rows: List<AccountLedgerRow>,
        val totalDebitsPaise: Long,
        val totalCreditsPaise: Long,
        val closingBalancePaise: Long,
        val closingBalanceSuffix: String
    )

    // 3. Trial Balance
    @Immutable
    data class TrialBalanceRow(
        val ledgerAccountId: Long,
        val accountCode: String,
        val accountName: String,
        val ledgerFolio: String,
        val classification: LedgerClassification,
        val debitBalancePaise: Long,
        val creditBalancePaise: Long
    )

    @Immutable
    data class TrialBalanceReport(
        val asAtEpochDay: Long,
        val rows: List<TrialBalanceRow>,
        val totalDebitPaise: Long,
        val totalCreditPaise: Long,
        val isAgreed: Boolean
    )

    // 4. Personal Balance Sheet (Indian Format)
    @Immutable
    data class BalanceSheetLine(
        val label: String,
        val amountPaise: Long
    )

    @Immutable
    data class BalanceSheetReport(
        val asAtEpochDay: Long,
        // ASSETS
        val nonCurrentInvestmentsPaise: Long,
        val nonCurrentPropertyAndOtherPaise: Long,
        val totalNonCurrentAssetsPaise: Long,
        val currentCashAndEquivalentsPaise: Long,
        val currentBankBalancesPaise: Long,
        val currentOtherAssetsPaise: Long,
        val totalCurrentAssetsPaise: Long,
        val totalAssetsPaise: Long,
        val assetDetails: List<BalanceSheetLine>,
        // LIABILITIES
        val nonCurrentLoansAndBorrowingsPaise: Long,
        val totalNonCurrentLiabilitiesPaise: Long,
        val currentCreditCardOutstandingPaise: Long,
        val currentOtherPayablesPaise: Long,
        val totalCurrentLiabilitiesPaise: Long,
        val totalLiabilitiesPaise: Long,
        val liabilityDetails: List<BalanceSheetLine>,
        // PERSONAL NET WORTH
        val openingCapitalPaise: Long,
        val additionalContributionsPaise: Long,
        val accumulatedSurplusPaise: Long,
        val drawingsPaise: Long,
        val totalNetWorthPaise: Long,
        val totalLiabilitiesAndNetWorthPaise: Long,
        val isReconciled: Boolean
    )

    // 5. Statement of Profit and Loss
    @Immutable
    data class ProfitAndLossReport(
        val fromEpochDay: Long,
        val toEpochDay: Long,
        // Income
        val salaryIncomePaise: Long,
        val businessIncomePaise: Long,
        val interestIncomePaise: Long,
        val dividendIncomePaise: Long,
        val capitalGainIncomePaise: Long,
        val otherIncomePaise: Long,
        val totalIncomePaise: Long,
        // Expenses
        val householdExpensesPaise: Long,
        val educationExpensesPaise: Long,
        val transportationExpensesPaise: Long,
        val utilitiesExpensesPaise: Long,
        val healthcareExpensesPaise: Long,
        val interestExpensesPaise: Long,
        val bankChargesPaise: Long,
        val investmentFeesPaise: Long,
        val taxesAndOtherExpensesPaise: Long,
        val totalExpensesPaise: Long,
        // Surplus / Deficit
        val surplusOrDeficitPaise: Long
    )

    // 6. Cash Flow Statement (Personal Adaptation: Operating, Investing, Financing)
    @Immutable
    data class CashFlowReport(
        val fromEpochDay: Long,
        val toEpochDay: Long,
        // Operating Activities
        val cashReceivedFromIncomePaise: Long,
        val interestAndDividendsReceivedPaise: Long,
        val cashPaidForExpensesPaise: Long,
        val netCashFromOperatingPaise: Long,
        // Investing Activities
        val investmentPurchasesOutflowPaise: Long,
        val investmentSalesInflowPaise: Long,
        val netCashFromInvestingPaise: Long,
        // Financing Activities
        val capitalAndOpeningContributionsPaise: Long,
        val loansReceivedInflowPaise: Long,
        val loanAndCardPrincipalRepaymentsOutflowPaise: Long,
        val netCashFromFinancingPaise: Long,
        // Reconciliation
        val netChangeInCashAndBankPaise: Long,
        val openingCashAndBankPaise: Long,
        val closingCashAndBankPaise: Long,
        val isReconciled: Boolean
    )

    /**
     * Generates the chronological Journal Book for the given period.
     */
    fun generateJournalBook(
        entries: List<JournalEntryEntity>,
        lines: List<JournalLineEntity>,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE
    ): JournalBookReport {
        val filteredEntries = entries.filter { it.dateEpochDay in fromEpochDay..toEpochDay }
            .sortedWith(compareBy<JournalEntryEntity> { it.dateEpochDay }.thenBy { it.id })
        val linesByEntry = lines.groupBy { it.journalEntryId }

        val rows = mutableListOf<JournalBookLineItem>()
        var totalDebits = 0L
        var totalCredits = 0L

        for (entry in filteredEntries) {
            val entryLines = linesByEntry[entry.id].orEmpty()
                .sortedWith(compareByDescending<JournalLineEntity> { it.debitPaise }.thenBy { it.id })

            for (line in entryLines) {
                val particulars = if (line.debitPaise > 0L) {
                    "${line.ledgerAccountName} A/c ... Dr."
                } else {
                    "    To ${line.ledgerAccountName} A/c"
                }
                rows.add(
                    JournalBookLineItem(
                        dateEpochDay = entry.dateEpochDay,
                        voucherNumber = entry.voucherNumber,
                        particulars = particulars,
                        ledgerFolio = line.ledgerFolio,
                        debitPaise = line.debitPaise,
                        creditPaise = line.creditPaise,
                        isNarrationRow = false
                    )
                )
                totalDebits += line.debitPaise
                totalCredits += line.creditPaise
            }
            rows.add(
                JournalBookLineItem(
                    dateEpochDay = entry.dateEpochDay,
                    voucherNumber = entry.voucherNumber,
                    particulars = "(Being ${entry.narration})",
                    ledgerFolio = "",
                    debitPaise = 0L,
                    creditPaise = 0L,
                    isNarrationRow = true
                )
            )
        }

        return JournalBookReport(
            entriesCount = filteredEntries.size,
            rows = rows,
            totalDebitPaise = totalDebits,
            totalCreditPaise = totalCredits,
            isBalanced = totalDebits == totalCredits
        )
    }

    /**
     * Generates the Individual Account Ledger for a specific LedgerAccountEntity.
     */
    fun generateAccountLedger(
        ledgerAccount: LedgerAccountEntity,
        allLines: List<JournalLineEntity>,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE
    ): IndividualAccountLedgerReport {
        val accountLines = allLines
            .filter { it.ledgerAccountId == ledgerAccount.id && it.dateEpochDay <= toEpochDay }
            .sortedWith(compareBy<JournalLineEntity> { it.dateEpochDay }.thenBy { it.journalEntryId }.thenBy { it.id })

        val priorLines = accountLines.filter { it.dateEpochDay < fromEpochDay }
        val periodLines = accountLines.filter { it.dateEpochDay in fromEpochDay..toEpochDay }

        val priorDebits = priorLines.sumOf { it.debitPaise }
        val priorCredits = priorLines.sumOf { it.creditPaise }

        val openingBalance = AccountingEngine.calculateClosingBalance(
            normalBalance = ledgerAccount.normalBalance,
            openingBalancePaise = 0L,
            totalDebitsPaise = priorDebits,
            totalCreditsPaise = priorCredits
        )

        var runningSigned = openingBalance
        var periodDebits = 0L
        var periodCredits = 0L
        val rows = mutableListOf<AccountLedgerRow>()

        for (line in periodLines) {
            periodDebits += line.debitPaise
            periodCredits += line.creditPaise
            runningSigned = when (ledgerAccount.normalBalance) {
                NormalBalance.DEBIT -> runningSigned + line.debitPaise - line.creditPaise
                NormalBalance.CREDIT -> runningSigned + line.creditPaise - line.debitPaise
            }
            val suffix = determineBalanceSuffix(ledgerAccount.normalBalance, runningSigned)
            val particulars = if (line.debitPaise > 0L) {
                "To ${line.lineNarration}"
            } else {
                "By ${line.lineNarration}"
            }
            rows.add(
                AccountLedgerRow(
                    dateEpochDay = line.dateEpochDay,
                    particulars = particulars,
                    voucherNumber = line.voucherNumber,
                    debitPaise = line.debitPaise,
                    creditPaise = line.creditPaise,
                    runningBalancePaise = kotlin.math.abs(runningSigned),
                    balanceSuffix = suffix
                )
            )
        }

        val closingSuffix = determineBalanceSuffix(ledgerAccount.normalBalance, runningSigned)

        return IndividualAccountLedgerReport(
            ledgerAccount = ledgerAccount,
            openingBalancePaise = openingBalance,
            rows = rows,
            totalDebitsPaise = periodDebits,
            totalCreditsPaise = periodCredits,
            closingBalancePaise = runningSigned,
            closingBalanceSuffix = closingSuffix
        )
    }

    private fun determineBalanceSuffix(normalBalance: NormalBalance, signedBalance: Long): String {
        if (signedBalance == 0L) return "Nil"
        return when (normalBalance) {
            NormalBalance.DEBIT -> if (signedBalance > 0L) "Dr" else "Cr"
            NormalBalance.CREDIT -> if (signedBalance > 0L) "Cr" else "Dr"
        }
    }

    /**
     * Generates the Trial Balance as at `asAtEpochDay` directly from posted journal lines.
     */
    fun generateTrialBalance(
        ledgerAccounts: List<LedgerAccountEntity>,
        allLines: List<JournalLineEntity>,
        asAtEpochDay: Long = Long.MAX_VALUE
    ): TrialBalanceReport {
        val relevantLines = allLines.filter { it.dateEpochDay <= asAtEpochDay }
        val linesByLedger = relevantLines.groupBy { it.ledgerAccountId }

        val rows = mutableListOf<TrialBalanceRow>()
        var totalDebit = 0L
        var totalCredit = 0L

        for (account in ledgerAccounts.sortedBy { it.code }) {
            val accLines = linesByLedger[account.id].orEmpty()
            val debits = accLines.sumOf { it.debitPaise }
            val credits = accLines.sumOf { it.creditPaise }
            val netDebit = debits - credits
            if (netDebit == 0L) continue

            val debitCol = if (netDebit > 0L) netDebit else 0L
            val creditCol = if (netDebit < 0L) -netDebit else 0L
            totalDebit += debitCol
            totalCredit += creditCol

            rows.add(
                TrialBalanceRow(
                    ledgerAccountId = account.id,
                    accountCode = account.code,
                    accountName = account.name,
                    ledgerFolio = "${account.classification.folioPrefix}-${account.id}",
                    classification = account.classification,
                    debitBalancePaise = debitCol,
                    creditBalancePaise = creditCol
                )
            )
        }

        return TrialBalanceReport(
            asAtEpochDay = asAtEpochDay,
            rows = rows,
            totalDebitPaise = totalDebit,
            totalCreditPaise = totalCredit,
            isAgreed = totalDebit == totalCredit
        )
    }

    /**
     * Generates the Statement of Profit and Loss for the period [fromEpochDay, toEpochDay].
     */
    fun generateProfitAndLoss(
        ledgerAccounts: List<LedgerAccountEntity>,
        allLines: List<JournalLineEntity>,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE
    ): ProfitAndLossReport {
        val ledgerById = ledgerAccounts.associateBy { it.id }
        val periodLines = allLines.filter { it.dateEpochDay in fromEpochDay..toEpochDay }

        fun netForClassification(classification: LedgerClassification): Long {
            val matchingLines = periodLines.filter { ledgerById[it.ledgerAccountId]?.classification == classification }
            val debits = matchingLines.sumOf { it.debitPaise }
            val credits = matchingLines.sumOf { it.creditPaise }
            return when (classification.normalBalance) {
                NormalBalance.CREDIT -> credits - debits
                NormalBalance.DEBIT -> debits - credits
            }
        }

        val salaryIncome = netForClassification(LedgerClassification.INCOME_SALARY)
        val businessIncome = netForClassification(LedgerClassification.INCOME_BUSINESS)
        val interestIncome = netForClassification(LedgerClassification.INCOME_INTEREST)
        val dividendIncome = netForClassification(LedgerClassification.INCOME_DIVIDEND)
        val capitalGainIncome = netForClassification(LedgerClassification.INCOME_CAPITAL_GAIN)
        val otherIncome = netForClassification(LedgerClassification.INCOME_OTHER)
        val totalIncome = salaryIncome + businessIncome + interestIncome + dividendIncome + capitalGainIncome + otherIncome

        val householdExp = netForClassification(LedgerClassification.EXPENSE_HOUSEHOLD)
        val educationExp = netForClassification(LedgerClassification.EXPENSE_EDUCATION)
        val transportExp = netForClassification(LedgerClassification.EXPENSE_TRANSPORT)
        val utilitiesExp = netForClassification(LedgerClassification.EXPENSE_UTILITIES)
        val healthcareExp = netForClassification(LedgerClassification.EXPENSE_HEALTHCARE)
        val interestExp = netForClassification(LedgerClassification.EXPENSE_INTEREST)
        val bankCharges = netForClassification(LedgerClassification.EXPENSE_BANK_CHARGES)
        val investmentFees = netForClassification(LedgerClassification.EXPENSE_INVESTMENT_FEES)
        val taxesAndOther = netForClassification(LedgerClassification.EXPENSE_TAXES) +
            netForClassification(LedgerClassification.EXPENSE_CAPITAL_LOSS) +
            netForClassification(LedgerClassification.EXPENSE_OTHER)

        val totalExpenses = householdExp + educationExp + transportExp + utilitiesExp +
            healthcareExp + interestExp + bankCharges + investmentFees + taxesAndOther

        val surplusOrDeficit = totalIncome - totalExpenses

        return ProfitAndLossReport(
            fromEpochDay = fromEpochDay,
            toEpochDay = toEpochDay,
            salaryIncomePaise = salaryIncome,
            businessIncomePaise = businessIncome,
            interestIncomePaise = interestIncome,
            dividendIncomePaise = dividendIncome,
            capitalGainIncomePaise = capitalGainIncome,
            otherIncomePaise = otherIncome,
            totalIncomePaise = totalIncome,
            householdExpensesPaise = householdExp,
            educationExpensesPaise = educationExp,
            transportationExpensesPaise = transportExp,
            utilitiesExpensesPaise = utilitiesExp,
            healthcareExpensesPaise = healthcareExp,
            interestExpensesPaise = interestExp,
            bankChargesPaise = bankCharges,
            investmentFeesPaise = investmentFees,
            taxesAndOtherExpensesPaise = taxesAndOther,
            totalExpensesPaise = totalExpenses,
            surplusOrDeficitPaise = surplusOrDeficit
        )
    }

    /**
     * Generates the Personal Balance Sheet as at `asAtEpochDay` following Section 10.D of the specification.
     * Verifies: Net Worth = Total Assets - Total Liabilities, and Total Assets = Total Liabilities + Net Worth.
     */
    fun generateBalanceSheet(
        ledgerAccounts: List<LedgerAccountEntity>,
        allLines: List<JournalLineEntity>,
        asAtEpochDay: Long = Long.MAX_VALUE
    ): BalanceSheetReport {
        val relevantLines = allLines.filter { it.dateEpochDay <= asAtEpochDay }
        val linesByLedger = relevantLines.groupBy { it.ledgerAccountId }

        fun accountBalance(account: LedgerAccountEntity): Long {
            val accLines = linesByLedger[account.id].orEmpty()
            val debits = accLines.sumOf { it.debitPaise }
            val credits = accLines.sumOf { it.creditPaise }
            return AccountingEngine.calculateClosingBalance(
                normalBalance = account.normalBalance,
                openingBalancePaise = 0L,
                totalDebitsPaise = debits,
                totalCreditsPaise = credits
            )
        }

        fun sumClassification(classification: LedgerClassification): Long {
            return ledgerAccounts
                .filter { it.classification == classification }
                .sumOf { accountBalance(it) }
        }

        // ASSETS
        val nonCurrentInvestments = sumClassification(LedgerClassification.ASSET_NON_CURRENT_INVESTMENT)
        val nonCurrentProperty = sumClassification(LedgerClassification.ASSET_NON_CURRENT_PROPERTY)
        val totalNonCurrentAssets = nonCurrentInvestments + nonCurrentProperty

        val currentCash = sumClassification(LedgerClassification.ASSET_CURRENT_CASH)
        val currentBank = sumClassification(LedgerClassification.ASSET_CURRENT_BANK)
        val currentOther = sumClassification(LedgerClassification.ASSET_CURRENT_OTHER)
        val totalCurrentAssets = currentCash + currentBank + currentOther

        val totalAssets = totalNonCurrentAssets + totalCurrentAssets

        val assetDetails = ledgerAccounts
            .filter { it.classification.isAsset }
            .map { BalanceSheetLine(it.name, accountBalance(it)) }
            .filter { it.amountPaise != 0L }

        // LIABILITIES
        val nonCurrentLoans = sumClassification(LedgerClassification.LIABILITY_NON_CURRENT_LOAN)
        val totalNonCurrentLiabilities = nonCurrentLoans

        val currentCreditCards = sumClassification(LedgerClassification.LIABILITY_CURRENT_CREDIT_CARD)
        val currentOtherPayables = sumClassification(LedgerClassification.LIABILITY_CURRENT_PAYABLE)
        val totalCurrentLiabilities = currentCreditCards + currentOtherPayables

        val totalLiabilities = totalNonCurrentLiabilities + totalCurrentLiabilities

        val liabilityDetails = ledgerAccounts
            .filter { it.classification.isLiability }
            .map { BalanceSheetLine(it.name, accountBalance(it)) }
            .filter { it.amountPaise != 0L }

        // PERSONAL NET WORTH
        val openingCapital = sumClassification(LedgerClassification.EQUITY_OPENING_CAPITAL)
        val additionalContributions = sumClassification(LedgerClassification.EQUITY_CONTRIBUTION)
        val drawings = sumClassification(LedgerClassification.EQUITY_DRAWINGS)

        val cumulativePnl = generateProfitAndLoss(
            ledgerAccounts = ledgerAccounts,
            allLines = allLines,
            fromEpochDay = Long.MIN_VALUE,
            toEpochDay = asAtEpochDay
        )
        val accumulatedSurplus = cumulativePnl.surplusOrDeficitPaise

        val totalNetWorth = openingCapital + additionalContributions + accumulatedSurplus - drawings
        val totalLiabilitiesAndNetWorth = totalLiabilities + totalNetWorth

        return BalanceSheetReport(
            asAtEpochDay = asAtEpochDay,
            nonCurrentInvestmentsPaise = nonCurrentInvestments,
            nonCurrentPropertyAndOtherPaise = nonCurrentProperty,
            totalNonCurrentAssetsPaise = totalNonCurrentAssets,
            currentCashAndEquivalentsPaise = currentCash,
            currentBankBalancesPaise = currentBank,
            currentOtherAssetsPaise = currentOther,
            totalCurrentAssetsPaise = totalCurrentAssets,
            totalAssetsPaise = totalAssets,
            assetDetails = assetDetails,
            nonCurrentLoansAndBorrowingsPaise = nonCurrentLoans,
            totalNonCurrentLiabilitiesPaise = totalNonCurrentLiabilities,
            currentCreditCardOutstandingPaise = currentCreditCards,
            currentOtherPayablesPaise = currentOtherPayables,
            totalCurrentLiabilitiesPaise = totalCurrentLiabilities,
            totalLiabilitiesPaise = totalLiabilities,
            liabilityDetails = liabilityDetails,
            openingCapitalPaise = openingCapital,
            additionalContributionsPaise = additionalContributions,
            accumulatedSurplusPaise = accumulatedSurplus,
            drawingsPaise = drawings,
            totalNetWorthPaise = totalNetWorth,
            totalLiabilitiesAndNetWorthPaise = totalLiabilitiesAndNetWorth,
            isReconciled = (totalAssets == totalLiabilitiesAndNetWorth) && (totalNetWorth == totalAssets - totalLiabilities)
        )
    }

    /**
     * Generates the Personal Cash Flow Statement (Operating, Investing, Financing Activities)
     * and reconciles Opening Cash & Bank to Closing Cash & Bank.
     */
    fun generateCashFlowStatement(
        ledgerAccounts: List<LedgerAccountEntity>,
        allLines: List<JournalLineEntity>,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE
    ): CashFlowReport {
        val ledgerById = ledgerAccounts.associateBy { it.id }
        val cashAndBankLedgerIds = ledgerAccounts
            .filter {
                it.classification == LedgerClassification.ASSET_CURRENT_CASH ||
                    it.classification == LedgerClassification.ASSET_CURRENT_BANK
            }
            .map { it.id }
            .toSet()

        fun cashAndBankBalanceAt(epochDay: Long): Long {
            val linesUpTo = allLines.filter { it.dateEpochDay <= epochDay && it.ledgerAccountId in cashAndBankLedgerIds }
            return linesUpTo.sumOf { it.debitPaise - it.creditPaise }
        }

        val openingCashAndBank = if (fromEpochDay == Long.MIN_VALUE) {
            0L
        } else {
            cashAndBankBalanceAt(fromEpochDay - 1)
        }
        val closingCashAndBank = cashAndBankBalanceAt(toEpochDay)

        val periodLines = allLines.filter { it.dateEpochDay in fromEpochDay..toEpochDay }
        val linesByEntry = periodLines.groupBy { it.journalEntryId }

        var operatingIncomeInflow = 0L
        var interestAndDividendInflow = 0L
        var operatingExpenseOutflow = 0L

        var investingPurchaseOutflow = 0L
        var investingSaleInflow = 0L

        var financingCapitalInflow = 0L
        var financingLoanInflow = 0L
        var financingRepaymentOutflow = 0L

        for ((_, entryLines) in linesByEntry) {
            val cashLines = entryLines.filter { it.ledgerAccountId in cashAndBankLedgerIds }
            val netCashDelta = cashLines.sumOf { it.debitPaise - it.creditPaise }
            if (netCashDelta == 0L) continue // Internal bank-to-cash transfer or non-cash credit card purchase

            val counterLines = entryLines.filter { it.ledgerAccountId !in cashAndBankLedgerIds }
            for (counter in counterLines) {
                val counterLedger = ledgerById[counter.ledgerAccountId] ?: continue
                val cls = counterLedger.classification
                // Impact of this counter-line on cash:
                // A credit on the counter-line corresponds to cash inflow (debit to cash),
                // a debit on the counter-line corresponds to cash outflow (credit to cash).
                val cashEffect = counter.creditPaise - counter.debitPaise

                when {
                    cls == LedgerClassification.INCOME_INTEREST || cls == LedgerClassification.INCOME_DIVIDEND -> {
                        if (cashEffect >= 0) interestAndDividendInflow += cashEffect
                        else operatingExpenseOutflow += -cashEffect
                    }
                    cls == LedgerClassification.INCOME_CAPITAL_GAIN || cls == LedgerClassification.EXPENSE_CAPITAL_LOSS -> {
                        if (cashEffect >= 0) investingSaleInflow += cashEffect
                        else investingSaleInflow -= -cashEffect
                    }
                    cls.isIncome -> {
                        if (cashEffect >= 0) operatingIncomeInflow += cashEffect
                        else operatingExpenseOutflow += -cashEffect
                    }
                    cls == LedgerClassification.EXPENSE_INVESTMENT_FEES -> {
                        if (cashEffect < 0) investingPurchaseOutflow += -cashEffect
                        else investingSaleInflow += cashEffect
                    }
                    cls.isExpense -> {
                        if (cashEffect <= 0) operatingExpenseOutflow += -cashEffect
                        else operatingExpenseOutflow -= cashEffect // Refund reducing expense
                    }
                    cls == LedgerClassification.ASSET_NON_CURRENT_INVESTMENT ||
                        cls == LedgerClassification.ASSET_NON_CURRENT_PROPERTY -> {
                        if (cashEffect < 0) investingPurchaseOutflow += -cashEffect
                        else investingSaleInflow += cashEffect
                    }
                    cls == LedgerClassification.ASSET_CURRENT_OTHER -> {
                        if (cashEffect < 0) operatingExpenseOutflow += -cashEffect
                        else operatingIncomeInflow += cashEffect
                    }
                    cls.isLiability -> {
                        if (cashEffect > 0) financingLoanInflow += cashEffect
                        else financingRepaymentOutflow += -cashEffect
                    }
                    cls.isEquity -> {
                        financingCapitalInflow += cashEffect
                    }
                }
            }
        }

        val netOperating = operatingIncomeInflow + interestAndDividendInflow - operatingExpenseOutflow
        val netInvesting = investingSaleInflow - investingPurchaseOutflow
        val netFinancing = financingCapitalInflow + financingLoanInflow - financingRepaymentOutflow
        val netChange = netOperating + netInvesting + netFinancing

        return CashFlowReport(
            fromEpochDay = fromEpochDay,
            toEpochDay = toEpochDay,
            cashReceivedFromIncomePaise = operatingIncomeInflow,
            interestAndDividendsReceivedPaise = interestAndDividendInflow,
            cashPaidForExpensesPaise = operatingExpenseOutflow,
            netCashFromOperatingPaise = netOperating,
            investmentPurchasesOutflowPaise = investingPurchaseOutflow,
            investmentSalesInflowPaise = investingSaleInflow,
            netCashFromInvestingPaise = netInvesting,
            capitalAndOpeningContributionsPaise = financingCapitalInflow,
            loansReceivedInflowPaise = financingLoanInflow,
            loanAndCardPrincipalRepaymentsOutflowPaise = financingRepaymentOutflow,
            netCashFromFinancingPaise = netFinancing,
            netChangeInCashAndBankPaise = netChange,
            openingCashAndBankPaise = openingCashAndBank,
            closingCashAndBankPaise = closingCashAndBank,
            isReconciled = (openingCashAndBank + netChange == closingCashAndBank)
        )
    }
}
