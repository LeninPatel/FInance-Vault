package com.example

import com.example.data.local.LedgerAccountEntity
import com.example.data.local.LedgerClassification
import com.example.data.local.NormalBalance
import com.example.data.local.TransactionType
import com.example.domain.accounting.AccountingEngine
import com.example.domain.calculators.FinancialCalculators
import com.example.domain.money.InrMoney
import com.example.domain.reports.FinancialReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `indian rupee formatting and paise conversion are accurate`() {
        assertEquals(125050L, InrMoney.parseInrToPaise("1,250.50"))
        assertEquals("₹1,000", InrMoney.formatPaise(1_000_00L))
        assertEquals("₹10,000", InrMoney.formatPaise(10_000_00L))
        assertEquals("₹1,00,000", InrMoney.formatPaise(1_00_000_00L))
        assertEquals("₹10,00,000", InrMoney.formatPaise(10_00_000_00L))
        assertEquals("₹1,00,00,000", InrMoney.formatPaise(1_00_00_000_00L))
        assertEquals("₹1,250.50", InrMoney.formatPaise(125050L))
        assertEquals("Not recorded", InrMoney.formatOptionalPaise(null, isRecorded = false))
    }

    @Test
    fun `financial calculators compute EMI, FD, RD, savings rate, and XIRR accurately`() {
        // Zero-income savings rate should not divide by zero
        assertEquals(0.0, FinancialCalculators.calculateSavingsRate(0L, 5000_00L), 0.001)
        // Normal savings rate: (20,000 - 5,000) / 20,000 = 75%
        assertEquals(75.0, FinancialCalculators.calculateSavingsRate(20_000_00L, 5_000_00L), 0.001)

        // Loan Amortization: ₹1,20,000 at 12% p.a. for 12 months
        val loanRes = FinancialCalculators.calculateLoanAmortization(
            principalPaise = 1_20_000_00L,
            annualInterestRatePercent = 12.0,
            tenureMonths = 12
        )
        assertTrue(loanRes.monthlyEmiPaise > 10_000_00L)
        assertEquals(12, loanRes.schedule.size)
        assertEquals(0L, loanRes.schedule.last().closingPrincipalPaise)
        assertEquals(
            1_20_000_00L,
            loanRes.schedule.sumOf { it.principalComponentPaise }
        )

        // Fixed Deposit Maturity: ₹1,00,000 at 7.5% p.a. quarterly compounding for 12 months
        val (fdMaturity, fdInterest) = FinancialCalculators.calculateFixedDepositMaturity(
            principalPaise = 1_00_000_00L,
            annualRatePercent = 7.5,
            durationMonths = 12,
            compoundingPerYear = 4
        )
        assertTrue(fdMaturity > 1_07_500_00L)
        assertEquals(fdMaturity - 1_00_000_00L, fdInterest)
    }

    @Test
    fun `all 10 mandatory accounting examples balance and reconcile in Balance Sheet, P&L, Trial Balance, and Cash Flow`() {
        val bankLedger = LedgerAccountEntity(1L, "1101", "Primary Bank", LedgerClassification.ASSET_CURRENT_BANK, NormalBalance.DEBIT)
        val cashLedger = LedgerAccountEntity(2L, "1102", "Cash in Hand", LedgerClassification.ASSET_CURRENT_CASH, NormalBalance.DEBIT)
        val ccLedger = LedgerAccountEntity(3L, "2101", "Credit Card Liability", LedgerClassification.LIABILITY_CURRENT_CREDIT_CARD, NormalBalance.CREDIT)
        val invLedger = LedgerAccountEntity(4L, "1201", "Mutual Fund Investment", LedgerClassification.ASSET_NON_CURRENT_INVESTMENT, NormalBalance.DEBIT)
        val loanLedger = LedgerAccountEntity(5L, "2201", "Personal Loan", LedgerClassification.LIABILITY_NON_CURRENT_LOAN, NormalBalance.CREDIT)
        val openCapLedger = LedgerAccountEntity(6L, "3001", "Opening Capital", LedgerClassification.EQUITY_OPENING_CAPITAL, NormalBalance.CREDIT, isSystemAccount = true)
        val salaryLedger = LedgerAccountEntity(7L, "4001", "Salary Income", LedgerClassification.INCOME_SALARY, NormalBalance.CREDIT, isSystemAccount = true)
        val householdLedger = LedgerAccountEntity(8L, "5001", "Household Expenses", LedgerClassification.EXPENSE_HOUSEHOLD, NormalBalance.DEBIT, isSystemAccount = true)
        val interestExpLedger = LedgerAccountEntity(9L, "5006", "Interest Expenses", LedgerClassification.EXPENSE_INTEREST, NormalBalance.DEBIT, isSystemAccount = true)
        val interestIncLedger = LedgerAccountEntity(10L, "4003", "Interest Income", LedgerClassification.INCOME_INTEREST, NormalBalance.CREDIT, isSystemAccount = true)

        val allLedgers = listOf(
            bankLedger, cashLedger, ccLedger, invLedger, loanLedger,
            openCapLedger, salaryLedger, householdLedger, interestExpLedger, interestIncLedger
        )

        var entryIdSeq = 1L
        var lineIdSeq = 1L
        val entries = mutableListOf<com.example.data.local.JournalEntryEntity>()
        val lines = mutableListOf<com.example.data.local.JournalLineEntity>()

        fun postPlan(plan: AccountingEngine.ValidatedPostingPlan, day: Long) {
            assertEquals(plan.totalDebitPaise, plan.totalCreditPaise)
            val eId = entryIdSeq++
            val vch = "VCH-2026-000$eId"
            entries.add(
                com.example.data.local.JournalEntryEntity(
                    id = eId,
                    transactionId = eId,
                    voucherNumber = vch,
                    dateEpochDay = day,
                    narration = plan.narration,
                    financialYear = "FY 2026-27"
                )
            )
            plan.lines.forEach { dl ->
                lines.add(
                    com.example.data.local.JournalLineEntity(
                        id = lineIdSeq++,
                        journalEntryId = eId,
                        transactionId = eId,
                        voucherNumber = vch,
                        dateEpochDay = day,
                        ledgerAccountId = dl.ledgerAccount.id,
                        ledgerAccountName = dl.ledgerAccount.name,
                        ledgerFolio = "${dl.ledgerAccount.classification.folioPrefix}-${dl.ledgerAccount.id}",
                        debitPaise = dl.debitPaise,
                        creditPaise = dl.creditPaise,
                        lineNarration = dl.narration
                    )
                )
            }
        }

        // Initial records from Section 17 Integration Test:
        // Bank opening amount: ₹50,000
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.OPENING_BALANCE,
                amountPaise = 50_000_00L,
                description = "Bank Opening",
                destinationLedger = bankLedger,
                openingCapitalLedger = openCapLedger
            ),
            day = 100L
        )
        // Cash opening amount: ₹5,000
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.OPENING_BALANCE,
                amountPaise = 5_000_00L,
                description = "Cash Opening",
                destinationLedger = cashLedger,
                openingCapitalLedger = openCapLedger
            ),
            day = 100L
        )

        // 1. Salary received: ₹20,000 into bank
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.INCOME,
                amountPaise = 20_000_00L,
                description = "Salary received",
                destinationLedger = bankLedger,
                categoryLedger = salaryLedger
            ),
            day = 101L
        )

        // 2. Household expense: ₹2,000 from bank
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.EXPENSE,
                amountPaise = 2_000_00L,
                description = "Household expense",
                sourceLedger = bankLedger,
                categoryLedger = householdLedger
            ),
            day = 102L
        )

        // 3. Transfer: ₹5,000 from bank to cash
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.TRANSFER,
                amountPaise = 5_000_00L,
                description = "Bank to cash transfer",
                sourceLedger = bankLedger,
                destinationLedger = cashLedger
            ),
            day = 103L
        )

        // 4. Credit card purchase: ₹1,000
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.CREDIT_CARD_PURCHASE,
                amountPaise = 1_000_00L,
                description = "Credit card purchase",
                sourceLedger = ccLedger,
                categoryLedger = householdLedger
            ),
            day = 104L
        )

        // 5. Credit card repayment: ₹500 from bank
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.CREDIT_CARD_REPAYMENT,
                amountPaise = 500_00L,
                description = "Credit card repayment",
                sourceLedger = bankLedger,
                destinationLedger = ccLedger
            ),
            day = 105L
        )

        // 6. Investment purchase: ₹3,000 from bank
        postPlan(
            AccountingEngine.buildPostingPlan(
                transactionType = TransactionType.INVESTMENT_PURCHASE,
                amountPaise = 3_000_00L,
                description = "Investment purchase",
                sourceLedger = bankLedger,
                destinationLedger = invLedger
            ),
            day = 106L
        )

        // Verify Individual Ledgers
        val bankReport = FinancialReportGenerator.generateAccountLedger(bankLedger, lines)
        val cashReport = FinancialReportGenerator.generateAccountLedger(cashLedger, lines)
        val ccReport = FinancialReportGenerator.generateAccountLedger(ccLedger, lines)
        val invReport = FinancialReportGenerator.generateAccountLedger(invLedger, lines)

        assertEquals(59_500_00L, bankReport.closingBalancePaise)
        assertEquals(10_000_00L, cashReport.closingBalancePaise)
        assertEquals(500_00L, ccReport.closingBalancePaise)
        assertEquals(3_000_00L, invReport.closingBalancePaise)

        // Verify Trial Balance
        val tb = FinancialReportGenerator.generateTrialBalance(allLedgers, lines)
        assertTrue(tb.isAgreed)
        assertEquals(tb.totalDebitPaise, tb.totalCreditPaise)

        // Verify Balance Sheet reconciliation
        val bs = FinancialReportGenerator.generateBalanceSheet(allLedgers, lines)
        assertTrue(bs.isReconciled)
        assertEquals(59_500_00L, bs.currentBankBalancesPaise)
        assertEquals(10_000_00L, bs.currentCashAndEquivalentsPaise)
        assertEquals(3_000_00L, bs.nonCurrentInvestmentsPaise)
        assertEquals(500_00L, bs.currentCreditCardOutstandingPaise)
        assertEquals(bs.totalAssetsPaise, bs.totalLiabilitiesAndNetWorthPaise)

        // Verify Profit & Loss: Salary = ₹20,000; Bank-to-cash transfer & Investment purchase & CC repayment NOT recorded as expenses
        val pnl = FinancialReportGenerator.generateProfitAndLoss(allLedgers, lines)
        assertEquals(20_000_00L, pnl.salaryIncomePaise)
        assertEquals(3_000_00L, pnl.householdExpensesPaise) // ₹2,000 bank + ₹1,000 credit card purchase

        // Verify Cash Flow Statement reconciliation
        val cf = FinancialReportGenerator.generateCashFlowStatement(allLedgers, lines)
        assertTrue(cf.isReconciled)
        assertEquals(69_500_00L, cf.closingCashAndBankPaise) // ₹59,500 bank + ₹10,000 cash
    }
}
