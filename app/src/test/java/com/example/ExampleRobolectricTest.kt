package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountType
import com.example.data.local.FinanceVaultDatabase
import com.example.data.local.InvestmentCategory
import com.example.data.local.TransactionType
import com.example.data.repository.FinanceVaultRepository
import com.example.domain.reports.FinancialReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: FinanceVaultDatabase
    private lateinit var repository: FinanceVaultRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinanceVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceVaultRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name and creator credit match specification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Finance Vault", context.getString(R.string.app_name))
        assertEquals("Made by Lenin and Shaswat", context.getString(R.string.creator_credit))
    }

    @Test
    fun `repository starts empty for real user mode and commits double-entry transactions atomically`() = runBlocking {
        repository.ensureInitialized()
        // Strict user-entered policy: zero fake accounts or transactions on fresh launch
        val initialAccounts = repository.observeAccounts(isDemo = false).first()
        val initialTransactions = repository.observeTransactions(isDemo = false).first()
        assertTrue(initialAccounts.isEmpty())
        assertTrue(initialTransactions.isEmpty())

        // User adds Bank account with ₹50,000 opening balance
        val bankId = repository.createAccount(
            name = "User Savings Bank",
            accountType = AccountType.SAVINGS,
            openingBalancePaise = 50_000_00L,
            isOpeningBalanceRecorded = true,
            openingDateEpochDay = 100L,
            isDemo = false
        )

        // User records ₹20,000 salary
        repository.recordTransaction(
            transactionType = TransactionType.INCOME,
            dateEpochDay = 101L,
            amountPaise = 20_000_00L,
            destinationAccountId = bankId,
            category = "Salary Income",
            description = "Monthly Salary",
            isDemo = false
        )

        // User purchases ₹3,000 investment from bank
        repository.addInvestment(
            name = "Index Fund",
            category = InvestmentCategory.MUTUAL_FUND,
            purchaseDateEpochDay = 102L,
            quantity = 1.0,
            units = 10.0,
            purchasePricePerUnitPaise = 300_00L,
            investedAmountPaise = 3_000_00L,
            paymentAccountId = bankId,
            isDemo = false
        )

        val ledgers = repository.observeLedgerAccounts(isDemo = false).first()
        val lines = repository.observeJournalLines(isDemo = false).first()

        val tb = FinancialReportGenerator.generateTrialBalance(ledgers, lines)
        val bs = FinancialReportGenerator.generateBalanceSheet(ledgers, lines)

        assertTrue(tb.isAgreed)
        assertTrue(bs.isReconciled)
        assertEquals(67_000_00L, bs.currentBankBalancesPaise)
        assertEquals(3_000_00L, bs.nonCurrentInvestmentsPaise)
        assertEquals(70_000_00L, bs.totalAssetsPaise)
        assertEquals(70_000_00L, bs.totalNetWorthPaise)
    }

    @Test
    fun `indian numbering system comma formatting formats first 3 digits then every 2 digits`() {
        assertEquals("999", com.example.domain.money.InrMoney.formatIndianInput("999"))
        assertEquals("1,000", com.example.domain.money.InrMoney.formatIndianInput("1000"))
        assertEquals("12,345", com.example.domain.money.InrMoney.formatIndianInput("12345"))
        assertEquals("1,00,000", com.example.domain.money.InrMoney.formatIndianInput("100000"))
        assertEquals("12,34,567", com.example.domain.money.InrMoney.formatIndianInput("1234567"))
        assertEquals("1,23,45,678.50", com.example.domain.money.InrMoney.formatIndianInput("12345678.50"))
    }

    @Test
    fun `document exporter creates valid PDF Excel and CSV files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository.ensureInitialized()
        val ledgers = repository.observeLedgerAccounts(isDemo = false).first()
        val lines = repository.observeJournalLines(isDemo = false).first()
        val entries = repository.observeJournalEntries(isDemo = false).first()

        val tb = FinancialReportGenerator.generateTrialBalance(ledgers, lines)
        val bs = FinancialReportGenerator.generateBalanceSheet(ledgers, lines)
        val pnl = FinancialReportGenerator.generateProfitAndLoss(ledgers, lines)
        val jb = FinancialReportGenerator.generateJournalBook(entries, lines)

        val pdfResult = com.example.domain.export.DocumentExporter.exportFinancialReportPdf(
            context = context,
            title = "Test Balance Sheet",
            reportText = "FINANCE VAULT STATEMENT\nTotal Assets: ₹50,000.00"
        )
        assertTrue(pdfResult.file.exists() && pdfResult.file.length() > 0 && pdfResult.file.extension == "pdf")

        val xlsxResult = com.example.domain.export.DocumentExporter.exportFinancialReportCsv(
            context = context,
            financialYear = "FY 2025-26",
            balanceSheet = bs,
            profitAndLoss = pnl,
            trialBalance = tb,
            journalBook = jb,
            asExcelExtension = true
        )
        assertTrue(xlsxResult.file.exists() && xlsxResult.file.length() > 0 && xlsxResult.file.extension == "xlsx")

        val csvResult = com.example.domain.export.DocumentExporter.exportFinancialReportCsv(
            context = context,
            financialYear = "FY 2025-26",
            balanceSheet = bs,
            profitAndLoss = pnl,
            trialBalance = tb,
            journalBook = jb,
            asExcelExtension = false
        )
        assertTrue(csvResult.file.exists() && csvResult.file.length() > 0 && csvResult.file.extension == "csv")
    }

    @get:org.junit.Rule
    val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `clicking Preview and Export PDF opens ExportPreviewDialog without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = com.example.ui.FinanceVaultViewModel(repository)
        composeTestRule.setContent {
            com.example.ui.theme.MyApplicationTheme {
                FinanceVaultApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        vm.selectTab(com.example.ui.MainTab.MORE)
        vm.navigateMore(com.example.ui.MoreDestination.REPORTS)
        composeTestRule.waitForIdle()
        vm.preparePdfExport(context)
        composeTestRule.waitForIdle()
        org.junit.Assert.assertNotNull(vm.exportPreviewResult.value)
    }

    @Test
    fun `search history retains last 5 unique terms and supports clear`() {
        val vm = com.example.ui.FinanceVaultViewModel(repository)
        listOf("Salary", "Groceries", "Rent", "Fuel", "Electricity", "Medical").forEach {
            vm.commitSearchQuery(it)
        }
        assertEquals(
            listOf("Medical", "Electricity", "Fuel", "Rent", "Groceries"),
            vm.searchHistory.value
        )
        vm.clearSearchHistory()
        assertTrue(vm.searchHistory.value.isEmpty())
    }

    @Test
    fun `bulk categorize and bulk delete via reversal keep double-entry ledgers balanced`() = runBlocking {
        repository.ensureInitialized()
        val bankId = repository.createAccount(
            name = "Primary Bank",
            accountType = AccountType.SAVINGS,
            openingBalancePaise = 1_00_000_00L,
            isOpeningBalanceRecorded = true,
            openingDateEpochDay = 200L,
            isDemo = false
        )
        val tx1 = repository.recordTransaction(
            transactionType = TransactionType.EXPENSE,
            dateEpochDay = 201L,
            amountPaise = 2_000_00L,
            sourceAccountId = bankId,
            category = "Other Expense",
            description = "Supermarket",
            isDemo = false
        )
        val tx2 = repository.recordTransaction(
            transactionType = TransactionType.EXPENSE,
            dateEpochDay = 202L,
            amountPaise = 3_000_00L,
            sourceAccountId = bankId,
            category = "Other Expense",
            description = "Vegetable Market",
            isDemo = false
        )

        val categorizedCount = repository.categorizeTransactionsBulk(setOf(tx1, tx2), "Household & Groceries")
        assertEquals(2, categorizedCount)
        assertEquals("Household & Groceries", repository. observeTransactions(false).first().first { it.id == tx1 }.category)

        val reversedCount = repository.reverseTransactionsBulk(setOf(tx1, tx2))
        assertEquals(2, reversedCount)

        val ledgers = repository.observeLedgerAccounts(isDemo = false).first()
        val lines = repository.observeJournalLines(isDemo = false).first()
        val tb = FinancialReportGenerator.generateTrialBalance(ledgers, lines)
        val bs = FinancialReportGenerator.generateBalanceSheet(ledgers, lines)
        assertTrue(tb.isAgreed)
        assertTrue(bs.isReconciled)
        assertEquals(1_00_000_00L, bs.currentBankBalancesPaise)
    }
}




