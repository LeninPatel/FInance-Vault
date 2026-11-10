package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.local.TransactionType
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.domain.reports.FinancialReportGenerator
import com.example.ui.FinanceVaultUiState
import com.example.ui.ReportTab
import com.example.ui.components.AssetDistributionChart
import com.example.ui.components.IncomeVsExpenseChartCard
import com.example.ui.components.VaultChooseSelector
import com.example.ui.components.VaultGlassCard
import com.example.ui.theme.VaultCardElevated
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultObsidianBg
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun FinancialReportsScreen(
    uiState: FinanceVaultUiState,
    selectedReportTab: ReportTab?,
    selectedLedgerAccountId: Long?,
    onSelectReportTab: (ReportTab) -> Unit,
    onSelectLedgerAccount: (Long) -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onExportCsv: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val isHidden = uiState.settings.isPrivacyModeEnabled
    val todayFormatted = remember { IndianFinancialYear.formatEpochDay(IndianFinancialYear.todayEpochDay()) }
    var reportTabExpandTrigger by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("financial_reports_screen"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header & Back button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Indian Financial Statements",
                            style = MaterialTheme.typography.titleLarge,
                            color = VaultTextPrimary
                        )
                    }
                }
            }
        }

        // Document Export Bar (PDF, Excel .xlsx, CSV)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Preview & Export PDF", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onExportExcel,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("export_excel_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Excel (.xlsx)", color = VaultCyan, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = VaultGold, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("CSV", color = VaultGold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Single Full-Width Financial Document Choose Tab
        item {
            VaultChooseSelector(
                label = "Financial Document",
                selectedItem = selectedReportTab,
                options = ReportTab.entries,
                onSelect = { onSelectReportTab(it) },
                itemLabel = { it.userLabel },
                itemIcon = { tab ->
                    when (tab) {
                        ReportTab.BALANCE_SHEET -> Icons.Default.AccountBalance
                        ReportTab.PROFIT_AND_LOSS -> Icons.AutoMirrored.Filled.TrendingUp
                        ReportTab.CASH_FLOW -> Icons.Default.SwapHoriz
                        ReportTab.TRIAL_BALANCE -> Icons.Default.FactCheck
                        ReportTab.ACCOUNT_LEDGER -> Icons.Default.AccountBalanceWallet
                        ReportTab.JOURNAL_BOOK -> Icons.AutoMirrored.Filled.ReceiptLong
                        ReportTab.SCHEDULES -> Icons.Default.Description
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Choose",
                testTag = "financial_document_choose_selector",
                autoExpandTrigger = reportTabExpandTrigger
            )
        }

        // Render active report
        when (selectedReportTab) {
            null -> {
                item {
                    VaultGlassCard(
                        accentColor = VaultCyan,
                        onClick = { reportTabExpandTrigger++ }
                    ) {
                        Text(
                            text = "Choose a Financial Statement",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }
                }
            }

            ReportTab.BALANCE_SHEET -> {
                item {
                    BalanceSheetDocumentView(
                        bs = uiState.balanceSheet,
                        dateLabel = todayFormatted,
                        isHidden = isHidden
                    )
                }
            }

            ReportTab.PROFIT_AND_LOSS -> {
                item {
                    ProfitAndLossDocumentView(
                        pnl = uiState.fyProfitAndLoss,
                        fyLabel = uiState.settings.activeFinancialYear,
                        dateLabel = todayFormatted,
                        isHidden = isHidden
                    )
                }
            }

            ReportTab.CASH_FLOW -> {
                item {
                    CashFlowDocumentView(
                        cf = uiState.fyCashFlow,
                        fyLabel = uiState.settings.activeFinancialYear,
                        dateLabel = todayFormatted,
                        isHidden = isHidden
                    )
                }
            }

            ReportTab.TRIAL_BALANCE -> {
                item {
                    TrialBalanceDocumentView(
                        tb = uiState.trialBalance,
                        dateLabel = todayFormatted,
                        isHidden = isHidden
                    )
                }
            }

            ReportTab.ACCOUNT_LEDGER -> {
                item {
                    AccountLedgerDocumentView(
                        uiState = uiState,
                        selectedLedgerId = selectedLedgerAccountId,
                        onSelectLedger = onSelectLedgerAccount,
                        isHidden = isHidden
                    )
                }
            }

            ReportTab.JOURNAL_BOOK -> {
                item {
                    JournalBookDocumentView(
                        jb = uiState.journalBook,
                        isHidden = isHidden
                    )
                }
            }

            ReportTab.SCHEDULES -> {
                item {
                    SupportingSchedulesView(uiState = uiState, isHidden = isHidden)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun BalanceSheetDocumentView(
    bs: FinancialReportGenerator.BalanceSheetReport,
    dateLabel: String,
    isHidden: Boolean
) {
    VaultGlassCard(accentColor = VaultEmerald) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "BALANCE SHEET AS AT ${dateLabel.uppercase()}",
                style = MaterialTheme.typography.titleLarge,
                color = VaultTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (bs.isReconciled) VaultEmerald else VaultCoral,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (bs.isReconciled) "Equation Verified: Total Assets = Total Liabilities + Net Worth"
                    else "Unbalanced",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bs.isReconciled) VaultEmerald else VaultCoral
                )
            }

            HorizontalDivider(color = VaultCardElevated)
            StatementRow("Particulars", "Amount (₹)", isHeader = true)
            HorizontalDivider(color = VaultCardElevated)

            // ASSETS
            StatementSectionTitle("ASSETS")
            StatementSubTitle("Non-Current Assets")
            StatementMoneyRow("  Investments", bs.nonCurrentInvestmentsPaise, isHidden)
            StatementMoneyRow("  Property and Other Long-Term Assets", bs.nonCurrentPropertyAndOtherPaise, isHidden)
            StatementSubTitle("Current Assets")
            StatementMoneyRow("  Cash and Cash Equivalents", bs.currentCashAndEquivalentsPaise, isHidden)
            StatementMoneyRow("  Bank Balances", bs.currentBankBalancesPaise, isHidden)
            StatementMoneyRow("  Other Current Assets", bs.currentOtherAssetsPaise, isHidden)
            HorizontalDivider(color = VaultCardElevated)
            StatementMoneyRow("TOTAL ASSETS", bs.totalAssetsPaise, isHidden, isBold = true, accent = VaultEmerald)
            HorizontalDivider(color = VaultCardElevated)

            // LIABILITIES
            StatementSectionTitle("LIABILITIES")
            StatementSubTitle("Non-Current Liabilities")
            StatementMoneyRow("  Loans and Borrowings", bs.nonCurrentLoansAndBorrowingsPaise, isHidden)
            StatementSubTitle("Current Liabilities")
            StatementMoneyRow("  Credit Card Outstanding", bs.currentCreditCardOutstandingPaise, isHidden)
            StatementMoneyRow("  Other Payables", bs.currentOtherPayablesPaise, isHidden)
            HorizontalDivider(color = VaultCardElevated)
            StatementMoneyRow("TOTAL LIABILITIES", bs.totalLiabilitiesPaise, isHidden, isBold = true, accent = VaultCoral)
            HorizontalDivider(color = VaultCardElevated)

            // PERSONAL NET WORTH
            StatementSectionTitle("PERSONAL NET WORTH")
            StatementMoneyRow("  Opening Capital", bs.openingCapitalPaise, isHidden)
            StatementMoneyRow("  Additional Contributions", bs.additionalContributionsPaise, isHidden)
            StatementMoneyRow("  Accumulated Surplus / (Deficit)", bs.accumulatedSurplusPaise, isHidden)
            StatementMoneyRow("  Less: Drawings, where applicable", bs.drawingsPaise, isHidden)
            HorizontalDivider(color = VaultCardElevated)
            StatementMoneyRow("TOTAL NET WORTH", bs.totalNetWorthPaise, isHidden, isBold = true, accent = VaultCyan)
            StatementMoneyRow(
                "TOTAL LIABILITIES AND NET WORTH",
                bs.totalLiabilitiesAndNetWorthPaise,
                isHidden,
                isBold = true,
                accent = VaultEmerald
            )
        }
    }
}

@Composable
private fun ProfitAndLossDocumentView(
    pnl: FinancialReportGenerator.ProfitAndLossReport,
    fyLabel: String,
    dateLabel: String,
    isHidden: Boolean
) {
    VaultGlassCard(accentColor = VaultCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PROFIT AND LOSS ACCOUNT FOR THE PERIOD ENDED ${dateLabel.uppercase()} ($fyLabel)",
                style = MaterialTheme.typography.titleMedium,
                color = VaultTextPrimary
            )
            HorizontalDivider(color = VaultCardElevated)
            StatementRow("Particulars", "Amount (₹)", isHeader = true)
            HorizontalDivider(color = VaultCardElevated)

            StatementSectionTitle("Income")
            StatementMoneyRow("  Salary Income", pnl.salaryIncomePaise, isHidden)
            StatementMoneyRow("  Business Income", pnl.businessIncomePaise, isHidden)
            StatementMoneyRow("  Interest Income", pnl.interestIncomePaise, isHidden)
            StatementMoneyRow("  Dividend Income", pnl.dividendIncomePaise, isHidden)
            StatementMoneyRow("  Other Income & Realized Gains", pnl.otherIncomePaise + pnl.capitalGainIncomePaise, isHidden)
            HorizontalDivider(color = VaultCardElevated)
            StatementMoneyRow("TOTAL INCOME", pnl.totalIncomePaise, isHidden, isBold = true, accent = VaultEmerald)
            HorizontalDivider(color = VaultCardElevated)

            StatementSectionTitle("Expenses")
            StatementMoneyRow("  Household Expenses", pnl.householdExpensesPaise, isHidden)
            StatementMoneyRow("  Education Expenses", pnl.educationExpensesPaise, isHidden)
            StatementMoneyRow("  Transportation Expenses", pnl.transportationExpensesPaise, isHidden)
            StatementMoneyRow("  Utilities", pnl.utilitiesExpensesPaise, isHidden)
            StatementMoneyRow("  Healthcare", pnl.healthcareExpensesPaise, isHidden)
            StatementMoneyRow("  Interest Expenses", pnl.interestExpensesPaise, isHidden)
            StatementMoneyRow("  Bank Charges", pnl.bankChargesPaise, isHidden)
            StatementMoneyRow("  Taxes and Other Expenses", pnl.taxesAndOtherExpensesPaise + pnl.investmentFeesPaise, isHidden)
            HorizontalDivider(color = VaultCardElevated)
            StatementMoneyRow("TOTAL EXPENSES", pnl.totalExpensesPaise, isHidden, isBold = true, accent = VaultCoral)
            HorizontalDivider(color = VaultCardElevated)

            StatementMoneyRow(
                "SURPLUS / DEFICIT FOR THE PERIOD",
                pnl.surplusOrDeficitPaise,
                isHidden,
                isBold = true,
                accent = if (pnl.surplusOrDeficitPaise >= 0) VaultEmerald else VaultCoral
            )
        }
    }
}

@Composable
private fun CashFlowDocumentView(
    cf: FinancialReportGenerator.CashFlowReport,
    fyLabel: String,
    dateLabel: String,
    isHidden: Boolean
) {
    VaultGlassCard(accentColor = VaultGold) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CASH FLOW STATEMENT FOR THE PERIOD ENDED ${dateLabel.uppercase()} ($fyLabel)",
                style = MaterialTheme.typography.titleMedium,
                color = VaultTextPrimary
            )
            HorizontalDivider(color = VaultCardElevated)

            StatementSectionTitle("1. Operating Activities")
            StatementMoneyRow("  Cash received from income", cf.cashReceivedFromIncomePaise, isHidden)
            StatementMoneyRow("  Interest & dividends received", cf.interestAndDividendsReceivedPaise, isHidden)
            StatementMoneyRow("  Less: Cash paid for expenses", -cf.cashPaidForExpensesPaise, isHidden)
            StatementMoneyRow("Net Cash from Operating Activities", cf.netCashFromOperatingPaise, isHidden, isBold = true)

            HorizontalDivider(color = VaultCardElevated)
            StatementSectionTitle("2. Investing Activities")
            StatementMoneyRow("  Investment sales / maturity proceeds", cf.investmentSalesInflowPaise, isHidden)
            StatementMoneyRow("  Less: Investment purchases", -cf.investmentPurchasesOutflowPaise, isHidden)
            StatementMoneyRow("Net Cash from Investing Activities", cf.netCashFromInvestingPaise, isHidden, isBold = true)

            HorizontalDivider(color = VaultCardElevated)
            StatementSectionTitle("3. Financing Activities")
            StatementMoneyRow("  Opening & capital contributions", cf.capitalAndOpeningContributionsPaise, isHidden)
            StatementMoneyRow("  Loans received", cf.loansReceivedInflowPaise, isHidden)
            StatementMoneyRow("  Less: Loan & card principal repayments", -cf.loanAndCardPrincipalRepaymentsOutflowPaise, isHidden)
            StatementMoneyRow("Net Cash from Financing Activities", cf.netCashFromFinancingPaise, isHidden, isBold = true)

            HorizontalDivider(color = VaultCardElevated)
            StatementMoneyRow("Net Movement in Cash & Bank", cf.netChangeInCashAndBankPaise, isHidden, isBold = true, accent = VaultCyan)
            StatementMoneyRow("Opening Cash & Bank Equivalents", cf.openingCashAndBankPaise, isHidden)
            StatementMoneyRow("Closing Cash & Bank Equivalents", cf.closingCashAndBankPaise, isHidden, isBold = true, accent = VaultEmerald)
        }
    }
}

@Composable
private fun TrialBalanceDocumentView(
    tb: FinancialReportGenerator.TrialBalanceReport,
    dateLabel: String,
    isHidden: Boolean
) {
    VaultGlassCard(accentColor = VaultEmerald) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "TRIAL BALANCE AS AT ${dateLabel.uppercase()}",
                style = MaterialTheme.typography.titleLarge,
                color = VaultTextPrimary
            )
            Text(
                text = if (tb.isAgreed) "Totals Agree: Total Debit = Total Credit" else "Totals Mismatch",
                style = MaterialTheme.typography.labelSmall,
                color = if (tb.isAgreed) VaultEmerald else VaultCoral
            )
            HorizontalDivider(color = VaultCardElevated)

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Name of Account", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelLarge, color = VaultTextSecondary)
                Text("Debit (₹)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = VaultTextSecondary)
                Text("Credit (₹)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = VaultTextSecondary)
            }
            HorizontalDivider(color = VaultCardElevated)

            if (tb.rows.isEmpty()) {
                Text("No posted ledger balances yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
            } else {
                tb.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(row.accountName, style = MaterialTheme.typography.bodySmall, color = VaultTextPrimary)
                            Text(row.ledgerFolio, style = MaterialTheme.typography.labelSmall, color = VaultTextMuted)
                        }
                        Text(
                            text = if (row.debitBalancePaise > 0) InrMoney.formatPaise(row.debitBalancePaise, isHidden = isHidden) else "—",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultEmerald
                        )
                        Text(
                            text = if (row.creditBalancePaise > 0) InrMoney.formatPaise(row.creditBalancePaise, isHidden = isHidden) else "—",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultCyan
                        )
                    }
                }
            }

            HorizontalDivider(color = VaultCardElevated)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("TOTAL", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.titleSmall, color = VaultTextPrimary)
                Text(
                    text = InrMoney.formatPaise(tb.totalDebitPaise, isHidden = isHidden),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = VaultEmerald
                )
                Text(
                    text = InrMoney.formatPaise(tb.totalCreditPaise, isHidden = isHidden),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = VaultCyan
                )
            }
        }
    }
}

@Composable
private fun AccountLedgerDocumentView(
    uiState: FinanceVaultUiState,
    selectedLedgerId: Long?,
    onSelectLedger: (Long) -> Unit,
    isHidden: Boolean
) {
    val activeLedgers = remember(uiState.ledgerAccounts, uiState.journalLines) {
        val idsWithActivity = uiState.journalLines.map { it.ledgerAccountId }.toSet()
        uiState.ledgerAccounts.filter { !it.isSystemAccount || it.id in idsWithActivity }
    }
    val currentLedger = activeLedgers.find { it.id == selectedLedgerId }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VaultChooseSelector(
            label = "Select Account Ledger",
            selectedItem = currentLedger,
            options = activeLedgers,
            onSelect = { onSelectLedger(it.id) },
            itemLabel = { "${it.name} (${it.classification.displayTitle})" },
            itemIcon = { Icons.Default.AccountBalanceWallet },
            placeholder = "Choose",
            testTag = "account_ledger_choose_selector"
        )

        if (currentLedger == null) {
            VaultGlassCard {
                Text(
                    "Select an Account Ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextSecondary
                )
            }
        } else {
            val report = remember(currentLedger, uiState.journalLines) {
                FinancialReportGenerator.generateAccountLedger(currentLedger, uiState.journalLines)
            }
            VaultGlassCard(accentColor = VaultCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "GENERAL LEDGER: ${currentLedger.name.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Classification: ${currentLedger.classification.displayTitle} | Normal Balance: ${currentLedger.normalBalance}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultCyan
                    )
                    HorizontalDivider(color = VaultCardElevated)

                    if (report.rows.isEmpty()) {
                        Text("No ledger postings recorded for this account yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                    } else {
                        report.rows.forEach { row ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultObsidianBg)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${IndianFinancialYear.formatShortEpochDay(row.dateEpochDay)} • ${row.voucherNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultTextMuted
                                    )
                                    Text(
                                        text = "Bal: ${InrMoney.formatPaise(row.runningBalancePaise, isHidden = isHidden)} ${row.balanceSuffix}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VaultCyan
                                    )
                                }
                                Text(row.particulars, style = MaterialTheme.typography.bodySmall, color = VaultTextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Debit: ${if (row.debitPaise > 0) InrMoney.formatPaise(row.debitPaise, isHidden = isHidden) else "—"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultEmerald
                                    )
                                    Text(
                                        text = "Credit: ${if (row.creditPaise > 0) InrMoney.formatPaise(row.creditPaise, isHidden = isHidden) else "—"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultGold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = VaultCardElevated)
                    StatementMoneyRow(
                        "Closing Balance (${report.closingBalanceSuffix})",
                        kotlin.math.abs(report.closingBalancePaise),
                        isHidden,
                        isBold = true,
                        accent = VaultEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalBookDocumentView(
    jb: FinancialReportGenerator.JournalBookReport,
    isHidden: Boolean
) {
    VaultGlassCard(accentColor = VaultGold) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "JOURNAL BOOK",
                style = MaterialTheme.typography.titleMedium,
                color = VaultTextPrimary
            )
            Text(
                text = "${jb.entriesCount} Voucher(s) • Balanced: ${if (jb.isBalanced) "YES (Dr = Cr)" else "NO"}",
                style = MaterialTheme.typography.labelSmall,
                color = VaultEmerald
            )
            HorizontalDivider(color = VaultCardElevated)

            if (jb.rows.isEmpty()) {
                Text("No journal entries posted yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
            } else {
                jb.rows.forEach { row ->
                    if (row.isNarrationRow) {
                        Text(
                            text = "${row.particulars} [${row.voucherNumber}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        HorizontalDivider(color = VaultCardElevated)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.4f)) {
                                Text(row.particulars, style = MaterialTheme.typography.bodySmall, color = VaultTextPrimary)
                                Text(
                                    "${IndianFinancialYear.formatShortEpochDay(row.dateEpochDay)} • Folio: ${row.ledgerFolio}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextMuted
                                )
                            }
                            Text(
                                text = if (row.debitPaise > 0) "Dr ${InrMoney.formatPaise(row.debitPaise, isHidden = isHidden)}" else "",
                                modifier = Modifier.weight(0.8f),
                                style = MaterialTheme.typography.labelMedium,
                                color = VaultEmerald
                            )
                            Text(
                                text = if (row.creditPaise > 0) "Cr ${InrMoney.formatPaise(row.creditPaise, isHidden = isHidden)}" else "",
                                modifier = Modifier.weight(0.8f),
                                style = MaterialTheme.typography.labelMedium,
                                color = VaultCyan
                            )
                        }
                    }
                }
            }

            StatementMoneyRow("TOTAL DEBITS", jb.totalDebitPaise, isHidden, isBold = true, accent = VaultEmerald)
            StatementMoneyRow("TOTAL CREDITS", jb.totalCreditPaise, isHidden, isBold = true, accent = VaultCyan)
        }
    }
}

@Composable
private fun SupportingSchedulesView(
    uiState: FinanceVaultUiState,
    isHidden: Boolean
) {
    val bs = uiState.balanceSheet
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Schedule 1: Statement of Changes in Net Worth", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                StatementMoneyRow("Opening Capital", bs.openingCapitalPaise, isHidden)
                StatementMoneyRow("Add: Additional Contributions", bs.additionalContributionsPaise, isHidden)
                StatementMoneyRow("Add: Period Surplus / (Deficit)", bs.accumulatedSurplusPaise, isHidden)
                StatementMoneyRow("Less: Drawings", bs.drawingsPaise, isHidden)
                StatementMoneyRow("Closing Personal Net Worth", bs.totalNetWorthPaise, isHidden, isBold = true, accent = VaultEmerald)
            }
        }

        VaultGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Schedule 2: Asset & Liability Schedule", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                if (bs.assetDetails.isEmpty() && bs.liabilityDetails.isEmpty()) {
                    Text("Not recorded yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                } else {
                    bs.assetDetails.forEach { line ->
                        StatementMoneyRow("Asset: ${line.label}", line.amountPaise, isHidden, accent = VaultEmerald)
                    }
                    bs.liabilityDetails.forEach { line ->
                        StatementMoneyRow("Liability: ${line.label}", line.amountPaise, isHidden, accent = VaultCoral)
                    }
                }
            }
        }

        VaultGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Schedule 3: Tax Payment Summary (${uiState.settings.activeFinancialYear})", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                val totalTax = uiState.taxRecords.sumOf { it.amountPaidPaise }
                StatementMoneyRow("Total Recorded Tax Payments", totalTax, isHidden, isBold = true, accent = VaultGold)
                Text(
                    text = "Do not use for official purposes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultTextMuted
                )
            }
        }
    }
}

private data class PlaceSpendRankItem(
    val placeLabel: String,
    val totalSpentPaise: Long,
    val transactionCount: Int
)

private data class CategorySpendItem(
    val category: String,
    val totalSpentPaise: Long,
    val transactionCount: Int,
    val sharePercent: Double,
    val color: Color
)

private data class MonthlyTrendBucket(
    val label: String,
    val yearMonthKey: Int,
    val incomePaise: Long,
    val expensePaise: Long
) {
    val netPaise: Long get() = incomePaise - expensePaise
}

private data class AccountFlowSummary(
    val accountName: String,
    val accountTypeLabel: String,
    val outflowPaise: Long,
    val inflowPaise: Long,
    val closingBalancePaise: Long
)

@Composable
fun AnalyticsScreen(
    uiState: FinanceVaultUiState,
    onOpenAddTransaction: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val isHidden = uiState.settings.isPrivacyModeEnabled
    val bs = uiState.balanceSheet
    val pnl = uiState.fyProfitAndLoss

    val spendTypes = remember {
        setOf(
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PURCHASE,
            TransactionType.GIFT_CARD_USAGE,
            TransactionType.TAX_PAYMENT,
            TransactionType.LOAN_REPAYMENT,
            TransactionType.BANK_CHARGE,
            TransactionType.INVESTMENT_PURCHASE,
            TransactionType.RD_CONTRIBUTION
        )
    }

    val incomeTypes = remember {
        setOf(
            TransactionType.INCOME,
            TransactionType.INTEREST_RECEIVED,
            TransactionType.DIVIDEND_RECEIVED,
            TransactionType.REFUND_RECEIVED,
            TransactionType.FD_MATURITY,
            TransactionType.INVESTMENT_SALE
        )
    }

    val activeTransactions = remember(uiState.transactions) {
        uiState.transactions.filter { !it.isReversed && it.amountPaise > 0L }
    }

    val activeSpendTxs = remember(activeTransactions, spendTypes) {
        activeTransactions.filter { it.transactionType in spendTypes }
    }

    val totalOutflowPaise = remember(activeSpendTxs) {
        activeSpendTxs.sumOf { it.amountPaise }
    }

    // 1. Spending Velocity, Average Ticket Size & Budget Utilization Metrics
    val (dailyVelocityPaise, avgTicketPaise, activeSpanDays) = remember(activeSpendTxs, totalOutflowPaise) {
        if (activeSpendTxs.isEmpty()) {
            Triple(0L, 0L, 1L)
        } else {
            val minDay = activeSpendTxs.minOf { it.dateEpochDay }
            val maxDay = maxOf(activeSpendTxs.maxOf { it.dateEpochDay }, IndianFinancialYear.todayEpochDay())
            val span = (maxDay - minDay + 1L).coerceAtLeast(1L)
            val velocity = totalOutflowPaise / span
            val avgTicket = totalOutflowPaise / activeSpendTxs.size.coerceAtLeast(1)
            Triple(velocity, avgTicket, span)
        }
    }

    val budgetUtilizationPct = remember(uiState.budgetStatuses, uiState.overallMonthlyBudgetStatus, uiState.currentMonthTotalSpentPaise) {
        val overall = uiState.overallMonthlyBudgetStatus
        if (overall != null && overall.limitPaise > 0L) {
            overall.usagePercent
        } else if (uiState.budgetStatuses.isNotEmpty()) {
            val totalLim = uiState.budgetStatuses.sumOf { it.limitPaise }.coerceAtLeast(1L)
            val totalSp = uiState.budgetStatuses.sumOf { it.spentPaise }
            (totalSp.toDouble() / totalLim.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    // 2. Category-wise Spending Breakdown & Percentages
    val categoryColors = remember {
        listOf(
            VaultEmerald,
            VaultCyan,
            VaultGold,
            VaultCoral,
            VaultPurple,
            Color(0xFF38BDF8),
            Color(0xFFF472B6),
            Color(0xFFA3E635)
        )
    }

    val categoryBreakdown = remember(activeSpendTxs, totalOutflowPaise, categoryColors) {
        val denom = totalOutflowPaise.coerceAtLeast(1L).toDouble()
        activeSpendTxs
            .groupBy { it.category.ifBlank { it.transactionType.userLabel } }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amountPaise } }
            .mapIndexed { idx, (cat, txs) ->
                val catTotal = txs.sumOf { it.amountPaise }
                CategorySpendItem(
                    category = cat,
                    totalSpentPaise = catTotal,
                    transactionCount = txs.size,
                    sharePercent = (catTotal.toDouble() / denom) * 100.0,
                    color = categoryColors[idx % categoryColors.size]
                )
            }
    }

    // 3. Monthly Income vs Expense Trend & Cash Flow Buckets (Last 6 Months)
    val monthlyTrends = remember(activeTransactions, spendTypes, incomeTypes) {
        val today = LocalDate.ofEpochDay(IndianFinancialYear.todayEpochDay())
        val last6Months = (5 downTo 0).map { offset ->
            val ym = today.minusMonths(offset.toLong())
            val key = ym.year * 100 + ym.monthValue
            val label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.US)} '${(ym.year % 100)}"
            key to label
        }
        val groupedByMonth = activeTransactions.groupBy { tx ->
            val d = LocalDate.ofEpochDay(tx.dateEpochDay)
            d.year * 100 + d.monthValue
        }
        last6Months.map { (ymKey, label) ->
            val txs = groupedByMonth[ymKey].orEmpty()
            val inc = txs.filter { it.transactionType in incomeTypes }.sumOf { it.amountPaise }
            val exp = txs.filter { it.transactionType in spendTypes }.sumOf { it.amountPaise }
            MonthlyTrendBucket(
                label = label,
                yearMonthKey = ymKey,
                incomePaise = inc,
                expensePaise = exp
            )
        }
    }

    // 4. Day-of-Week Spending Velocity Distribution (Mon..Sun)
    val weekdaySpending = remember(activeSpendTxs) {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val totals = LongArray(7)
        for (tx in activeSpendTxs) {
            val dowIndex = (LocalDate.ofEpochDay(tx.dateEpochDay).dayOfWeek.value - 1).coerceIn(0, 6)
            totals[dowIndex] += tx.amountPaise
        }
        labels.mapIndexed { idx, dayLabel -> dayLabel to totals[idx] }
    }

    // 5. Account-Wise Outflow & Inflow Comparison
    val accountComparisons = remember(uiState.accountsWithBalances, activeTransactions, spendTypes, incomeTypes) {
        uiState.accountsWithBalances
            .filter { !it.account.isArchived }
            .map { accWithBal ->
                val accId = accWithBal.account.id
                val outflow = activeTransactions
                    .filter { it.sourceAccountId == accId && (it.transactionType in spendTypes || it.transactionType == TransactionType.TRANSFER) }
                    .sumOf { it.amountPaise }
                val inflow = activeTransactions
                    .filter { it.destinationAccountId == accId && (it.transactionType in incomeTypes || it.transactionType == TransactionType.TRANSFER) }
                    .sumOf { it.amountPaise }
                AccountFlowSummary(
                    accountName = accWithBal.account.name,
                    accountTypeLabel = accWithBal.account.accountType.displayName,
                    outflowPaise = outflow,
                    inflowPaise = inflow,
                    closingBalancePaise = accWithBal.closingBalancePaise
                )
            }
            .sortedByDescending { it.outflowPaise + it.inflowPaise }
    }

    // 6. Ranked Spending by Place / Location
    val placeRankings = remember(activeSpendTxs) {
        activeSpendTxs
            .groupBy { tx ->
                when {
                    tx.locationName.isNotBlank() -> tx.locationName.trim()
                    tx.latitude != null && tx.longitude != null ->
                        String.format(Locale.US, "GPS %.4f, %.4f", tx.latitude, tx.longitude)
                    else -> "Unspecified Place"
                }
            }
            .map { (place, list) ->
                PlaceSpendRankItem(
                    placeLabel = place,
                    totalSpentPaise = list.sumOf { it.amountPaise },
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.totalSpentPaise }
    }
    val totalPlaceSpendPaise = remember(placeRankings) {
        placeRankings.sumOf { it.totalSpentPaise }.coerceAtLeast(1L)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Analytics & Financial Performance",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenAddTransaction,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("analytics_add_tx_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", maxLines = 1)
                }
            }
        }

        // SECTION 1: Daily Spending Velocity, Average Ticket Size & Budget Utilization KPI Strip
        item {
            VaultGlassCard(
                accentColor = VaultEmerald,
                modifier = Modifier.testTag("analytics_velocity_kpi_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = VaultEmerald, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Spending Velocity & Ticket Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsKpiTile(
                            title = "Daily Velocity",
                            value = "${InrMoney.formatPaise(dailyVelocityPaise, isHidden = isHidden)}/day",
                            subtitle = "Over $activeSpanDays active day(s)",
                            accent = VaultCyan,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsKpiTile(
                            title = "Avg Ticket Size",
                            value = InrMoney.formatPaise(avgTicketPaise, isHidden = isHidden),
                            subtitle = "${activeSpendTxs.size} outflow tx(s)",
                            accent = VaultGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsKpiTile(
                            title = "Month Spend",
                            value = InrMoney.formatPaise(uiState.currentMonthTotalSpentPaise, isHidden = isHidden),
                            subtitle = "Savings: ${uiState.monthlySavingsRatePercent}%",
                            accent = VaultEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        val budgetAccent = when {
                            budgetUtilizationPct >= 100.0 -> VaultCoral
                            budgetUtilizationPct >= 80.0 -> VaultGold
                            else -> VaultEmerald
                        }
                        AnalyticsKpiTile(
                            title = "Budget Used",
                            value = if (uiState.budgets.isEmpty()) "No Limit" else String.format(Locale.US, "%.1f%%", budgetUtilizationPct),
                            subtitle = if (uiState.budgets.isEmpty()) "Set in Budgets & Goals" else "${uiState.budgets.size} active budget(s)",
                            accent = budgetAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // SECTION 2: Category-Wise Spending Breakdown Chart (Multi-Segment Donut + Percentages)
        item {
            VaultGlassCard(
                accentColor = VaultPurple,
                modifier = Modifier.testTag("analytics_category_breakdown_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = VaultPurple, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Category-Wise Spending Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }

                    if (categoryBreakdown.isEmpty()) {
                        Text(
                            text = "No expense categories recorded yet. Add a transaction to view category percentages and donut chart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextSecondary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(132.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(120.dp)) {
                                    val strokeWidth = 22.dp.toPx()
                                    var startAngle = -90f
                                    categoryBreakdown.forEach { seg ->
                                        val sweep = ((seg.sharePercent / 100.0) * 360.0).toFloat().coerceAtLeast(2f)
                                        drawArc(
                                            color = seg.color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                            size = Size(size.width, size.height)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${categoryBreakdown.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VaultTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Categories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultTextSecondary
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categoryBreakdown.take(4).forEach { seg ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(seg.color)
                                            )
                                            Text(
                                                text = seg.category,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = VaultTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = String.format(Locale.US, "%.1f%%", seg.sharePercent),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = seg.color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = VaultCardElevated)

                        categoryBreakdown.forEach { item ->
                            val progress = (item.sharePercent / 100.0).toFloat().coerceIn(0.03f, 1f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultObsidianBg)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.category,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = VaultTextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.transactionCount} tx(s) • ${String.format(Locale.US, "%.1f%%", item.sharePercent)} of total spend",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VaultTextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = InrMoney.formatPaise(item.totalSpentPaise, isHidden = isHidden),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = item.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = item.color,
                                    trackColor = VaultCardElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: Monthly Income vs Expense Trend & Net Cash Flow Chart (6-Month Trend)
        item {
            VaultGlassCard(
                accentColor = VaultCyan,
                modifier = Modifier.testTag("analytics_monthly_cashflow_trend_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Monthly Income vs Expense & Cash Flow Trend",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }

                    val maxMonthlyVal = monthlyTrends.maxOfOrNull { maxOf(it.incomePaise, it.expensePaise) }?.coerceAtLeast(1L) ?: 1L

                    monthlyTrends.forEach { bucket ->
                        val incRatio = (bucket.incomePaise.toDouble() / maxMonthlyVal.toDouble()).toFloat().coerceIn(0f, 1f)
                        val expRatio = (bucket.expensePaise.toDouble() / maxMonthlyVal.toDouble()).toFloat().coerceIn(0f, 1f)
                        val netPositive = bucket.netPaise >= 0L

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultObsidianBg)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bucket.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VaultTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Net: ${if (netPositive) "+" else ""}${InrMoney.formatPaise(bucket.netPaise, isHidden = isHidden)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (netPositive) VaultEmerald else VaultCoral,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("In", style = MaterialTheme.typography.labelSmall, color = VaultEmerald, modifier = Modifier.width(24.dp))
                                LinearProgressIndicator(
                                    progress = { incRatio.coerceAtLeast(if (bucket.incomePaise > 0L) 0.04f else 0f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = VaultEmerald,
                                    trackColor = VaultCardElevated
                                )
                                Text(
                                    text = InrMoney.formatPaise(bucket.incomePaise, isHidden = isHidden),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Out", style = MaterialTheme.typography.labelSmall, color = VaultCoral, modifier = Modifier.width(24.dp))
                                LinearProgressIndicator(
                                    progress = { expRatio.coerceAtLeast(if (bucket.expensePaise > 0L) 0.04f else 0f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = VaultCoral,
                                    trackColor = VaultCardElevated
                                )
                                Text(
                                    text = InrMoney.formatPaise(bucket.expensePaise, isHidden = isHidden),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 4: Day-of-Week Spending Velocity & Budget Utilization Breakdown
        item {
            VaultGlassCard(
                accentColor = VaultGold,
                modifier = Modifier.testTag("analytics_budget_and_weekday_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = VaultGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Day-of-Week Velocity & Budget Utilization",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }

                    val maxDaySpend = weekdaySpending.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(VaultObsidianBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekdaySpending.forEach { (dayLabel, dayPaise) ->
                            val barFraction = (dayPaise.toDouble() / maxDaySpend.toDouble()).toFloat().coerceIn(0.08f, 1f)
                            val isPeak = dayPaise > 0L && dayPaise == maxDaySpend
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(64.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((64f * barFraction).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (isPeak) VaultGold else VaultCyan.copy(alpha = 0.75f))
                                    )
                                }
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPeak) VaultGold else VaultTextSecondary,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (uiState.budgetStatuses.isNotEmpty()) {
                        HorizontalDivider(color = VaultCardElevated)
                        Text(
                            text = "Active Budget Utilization",
                            style = MaterialTheme.typography.labelLarge,
                            color = VaultTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        uiState.budgetStatuses.forEach { status ->
                            val pct = status.usagePercent
                            val barColor = when {
                                pct >= 100.0 -> VaultCoral
                                pct >= 80.0 -> VaultGold
                                else -> VaultEmerald
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${status.budget.name} (${status.budget.category})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VaultTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${InrMoney.formatPaise(status.spentPaise, isHidden = isHidden)} / ${InrMoney.formatPaise(status.limitPaise, isHidden = isHidden)} (${String.format(Locale.US, "%.0f%%", pct)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = barColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (pct / 100.0).toFloat().coerceIn(0.02f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = barColor,
                                    trackColor = VaultCardElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 5: Account-Wise Outflow Comparison & Investment vs. Debt Health Gauges
        item {
            VaultGlassCard(
                accentColor = VaultEmerald,
                modifier = Modifier.testTag("analytics_account_comparison_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = VaultEmerald, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Account-Wise Outflow & Inflow Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }

                    if (accountComparisons.isEmpty()) {
                        Text(
                            text = "No active accounts available yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextSecondary
                        )
                    } else {
                        val maxAccOutflow = accountComparisons.maxOfOrNull { maxOf(it.outflowPaise, it.inflowPaise, 1L) } ?: 1L
                        accountComparisons.forEach { acc ->
                            val outProg = (acc.outflowPaise.toDouble() / maxAccOutflow.toDouble()).toFloat().coerceIn(0f, 1f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultObsidianBg)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = acc.accountName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = VaultTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${acc.accountTypeLabel} • Closing: ${InrMoney.formatPaise(acc.closingBalancePaise, isHidden = isHidden)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VaultTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Out: ${InrMoney.formatPaise(acc.outflowPaise, isHidden = isHidden)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VaultCoral,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "In: ${InrMoney.formatPaise(acc.inflowPaise, isHidden = isHidden)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VaultEmerald
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { outProg.coerceAtLeast(if (acc.outflowPaise > 0L) 0.04f else 0f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = VaultCoral,
                                    trackColor = VaultCardElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 6: Investment vs. Debt Health & Coverage Gauges
        item {
            VaultGlassCard(
                accentColor = VaultCyan,
                modifier = Modifier.testTag("analytics_investment_debt_health_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Investment vs. Debt & Solvency Health Gauges",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }

                    val totalInvestmentsPaise = bs.nonCurrentInvestmentsPaise
                    val totalLiquidPaise = bs.currentCashAndEquivalentsPaise + bs.currentBankBalancesPaise
                    val totalDebtPaise = bs.totalLiabilitiesPaise
                    val invVsDebtDenom = (totalInvestmentsPaise + totalDebtPaise).coerceAtLeast(1L).toDouble()
                    val investmentShare = (totalInvestmentsPaise.toDouble() / invVsDebtDenom).toFloat().coerceIn(0f, 1f)
                    val debtToAssetPct = InrMoney.percentageRatio(bs.totalLiabilitiesPaise, bs.totalAssetsPaise)
                    val solvencyHealthy = totalDebtPaise == 0L || bs.totalAssetsPaise >= totalDebtPaise * 2

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(VaultObsidianBg)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Investments: ${InrMoney.formatPaise(totalInvestmentsPaise, isHidden = isHidden)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VaultEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Debt: ${InrMoney.formatPaise(totalDebtPaise, isHidden = isHidden)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (totalDebtPaise > 0L) VaultCoral else VaultTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (totalInvestmentsPaise == 0L && totalDebtPaise == 0L) 0.5f else investmentShare },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = VaultEmerald,
                            trackColor = if (totalDebtPaise > 0L) VaultCoral else VaultCardElevated
                        )
                        Text(
                            text = if (totalDebtPaise == 0L) {
                                "Debt-Free Status • 100% Solvency Cushion"
                            } else {
                                "Liquid Reserves (${InrMoney.formatPaise(totalLiquidPaise, isHidden = isHidden)}) vs Debt Ratio: $debtToAssetPct% of Total Assets"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (solvencyHealthy) VaultEmerald else VaultGold
                        )
                    }

                    StatementRow("FY Savings Rate", "${uiState.fySavingsRatePercent}%")
                    StatementRow("Current Month Savings Rate", "${uiState.monthlySavingsRatePercent}%")
                    StatementRow("Liability-to-Asset Ratio", "$debtToAssetPct%")
                }
            }
        }

        // Ranked Spending by Place / Location (Descending: Most to Least)
        item {
            VaultGlassCard(
                accentColor = VaultCyan,
                modifier = Modifier.testTag("analytics_place_spending_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = VaultCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Spending by Place (Most to Least)",
                                style = MaterialTheme.typography.titleMedium,
                                color = VaultTextPrimary
                            )
                        }
                    }

                    if (placeRankings.isEmpty()) {
                        Text(
                            text = "No spending places recorded yet. Tap + Add to record a transaction with a location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextSecondary
                        )
                    } else {
                        placeRankings.forEachIndexed { index, item ->
                            val rank = index + 1
                            val sharePct = (item.totalSpentPaise.toDouble() / totalPlaceSpendPaise.toDouble()) * 100.0
                            val shareProgress = (sharePct / 100.0).toFloat().coerceIn(0.03f, 1f)
                            val badgeColor = when (rank) {
                                1 -> VaultGold
                                2 -> VaultEmerald
                                3 -> VaultCyan
                                else -> VaultTextSecondary
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultObsidianBg)
                                    .padding(10.dp)
                                    .testTag("place_rank_row_$rank"),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#$rank",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.placeLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = VaultTextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.transactionCount} transaction(s) • ${String.format(Locale.US, "%.1f%%", sharePct)} of spend",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VaultTextSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = InrMoney.formatPaise(item.totalSpentPaise, isHidden = isHidden),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (rank == 1) VaultGold else VaultEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { shareProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = badgeColor,
                                    trackColor = VaultCardElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            IncomeVsExpenseChartCard(
                incomePaise = pnl.totalIncomePaise,
                expensePaise = pnl.totalExpensesPaise,
                periodLabel = uiState.settings.activeFinancialYear,
                isHidden = isHidden
            )
        }

        item {
            AssetDistributionChart(
                cashPaise = bs.currentCashAndEquivalentsPaise,
                bankPaise = bs.currentBankBalancesPaise,
                investmentsPaise = bs.nonCurrentInvestmentsPaise,
                otherAssetsPaise = bs.currentOtherAssetsPaise + bs.nonCurrentPropertyAndOtherPaise,
                isHidden = isHidden
            )
        }
    }
}

@Composable
private fun AnalyticsKpiTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(VaultObsidianBg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = VaultTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = VaultTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatementSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = VaultCyan,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun StatementSubTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = VaultTextSecondary
    )
}

@Composable
private fun StatementRow(left: String, right: String, isHeader: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = left,
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            color = if (isHeader) VaultTextSecondary else VaultTextPrimary
        )
        Text(
            text = right,
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = VaultTextPrimary
        )
    }
}

@Composable
private fun StatementMoneyRow(
    label: String,
    amountPaise: Long,
    isHidden: Boolean,
    isBold: Boolean = false,
    accent: Color = VaultTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (isBold) accent else VaultTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = InrMoney.formatPaise(amountPaise, showZeroPaise = true, isHidden = isHidden),
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            color = accent
        )
    }
}
