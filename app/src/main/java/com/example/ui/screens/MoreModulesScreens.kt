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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountType
import com.example.data.local.AuditLogEntity
import com.example.data.local.BillingFrequency
import com.example.data.local.BudgetPeriod
import com.example.data.local.GoalType
import com.example.data.local.LoanEntity
import com.example.data.local.LoanType
import com.example.data.local.TaxType
import com.example.data.local.ThemeMode
import com.example.data.local.TransactionType
import com.example.domain.security.BiometricAuthHelper
import com.example.domain.calculators.FinancialCalculators
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubscriptionEntity
import com.example.domain.reminder.TransactionRecallReminderManager
import com.example.ui.BudgetAlertTier
import com.example.ui.FinanceVaultUiState
import com.example.ui.MoreDestination
import com.example.ui.components.CustomRecallRemindersManagementCard
import com.example.ui.components.VaultGlassCard
import com.example.ui.theme.VaultCardElevated
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultObsidianBg
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.components.IndianCurrencyVisualTransformation
import com.example.ui.components.VaultChooseSelector
import com.example.ui.components.VaultOptionTogglePill
import com.example.ui.components.VaultToggleSwitch
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.categoryIcon
import com.example.ui.components.dismissKeyboardOnSwipeDown
import com.example.ui.components.goalTypeIcon
import com.example.ui.components.loanTypeIcon
import com.example.ui.components.taxTypeIcon

@Composable
fun MoreHubScreen(
    uiState: FinanceVaultUiState,
    onSelectDestination: (MoreDestination) -> Unit
) {
    val destinations = remember {
        MoreDestination.entries.filter { it != MoreDestination.HUB }
    }
    var showAuditTrailDialog by remember { mutableStateOf(false) }

    if (showAuditTrailDialog) {
        AuditTrailFullDialog(
            auditLogs = uiState.auditLogs,
            onDismiss = { showAuditTrailDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnSwipeDown()
            .testTag("more_hub_screen"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Finance Vault Modules",
                style = MaterialTheme.typography.headlineMedium,
                color = VaultTextPrimary
            )
        }

        items(destinations) { dest ->
            val destIcon = when (dest) {
                MoreDestination.REPORTS -> Icons.Default.Assessment
                MoreDestination.ANALYTICS -> Icons.Default.AutoGraph
                MoreDestination.LOANS -> Icons.Default.CreditCard
                MoreDestination.BUDGETS_GOALS -> Icons.Default.Savings
                MoreDestination.GIFT_CARDS -> Icons.Default.CardGiftcard
                MoreDestination.SUBSCRIPTIONS -> Icons.Default.Autorenew
                MoreDestination.TAXES -> Icons.Default.Gavel
                MoreDestination.SETTINGS -> Icons.Default.Settings
                MoreDestination.HUB -> Icons.Default.Settings
            }
            val destAccent = when (dest) {
                MoreDestination.REPORTS -> VaultEmerald
                MoreDestination.ANALYTICS -> VaultCyan
                MoreDestination.LOANS -> VaultCoral
                MoreDestination.BUDGETS_GOALS -> VaultEmerald
                MoreDestination.GIFT_CARDS -> VaultGold
                MoreDestination.SUBSCRIPTIONS -> VaultCyan
                MoreDestination.TAXES -> VaultGold
                MoreDestination.SETTINGS -> VaultCyan
                MoreDestination.HUB -> VaultEmerald
            }

            VaultGlassCard(
                accentColor = if (dest == MoreDestination.REPORTS) VaultEmerald else VaultCardElevated,
                onClick = { onSelectDestination(dest) },
                modifier = Modifier.testTag(dest.testTag)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(destAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = destIcon,
                                contentDescription = dest.title,
                                tint = destAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dest.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = VaultTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = destAccent
                    )
                }
            }
        }

        // Interactive Account Audit Trail Card in More window
        item {
            VaultGlassCard(
                accentColor = VaultEmerald,
                onClick = { showAuditTrailDialog = true },
                modifier = Modifier.testTag("more_audit_trail_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Account Audit Trail (${uiState.auditLogs.size} events)",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Audit Trail",
                        tint = VaultEmerald
                    )
                }
            }
        }

        // Creator Identity Card
        item {
            VaultGlassCard(accentColor = VaultCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Finance Vault",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Made by Lenin and Shaswat",
                        style = MaterialTheme.typography.labelLarge,
                        color = VaultEmerald,
                        modifier = Modifier.testTag("more_creator_credit")
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(68.dp)) }
    }
}

@Composable
fun LoansAndCreditCardsScreen(
    uiState: FinanceVaultUiState,
    onAddLoan: (
        name: String,
        loanType: LoanType,
        lenderName: String,
        principalPaise: Long,
        annualInterestRatePercent: Double,
        tenureMonths: Int,
        startDateEpochDay: Long,
        disbursedToAccountId: Long?,
        notes: String
    ) -> Unit,
    onRecordEmiPayment: (
        loanId: Long,
        sourceAccountId: Long,
        totalEmiPaise: Long,
        principalPaise: Long,
        interestPaise: Long,
        loanName: String
    ) -> Unit,
    onAddCustomOption: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var selectedScheduleLoan by remember { mutableStateOf<LoanEntity?>(null) }
    val isHidden = uiState.settings.isPrivacyModeEnabled

    val creditCards = remember(uiState.accountsWithBalances) {
        uiState.accountsWithBalances.filter { it.account.accountType == AccountType.CREDIT_CARD }
    }
    val paymentBankAccounts = remember(uiState.accountsWithBalances) {
        uiState.accountsWithBalances.map { it.account }.filter { !it.isArchived && it.accountType != AccountType.CREDIT_CARD }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loans_screen"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Loans & Credit Cards", style = MaterialTheme.typography.titleLarge, color = VaultTextPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddLoanDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_loan_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Loan", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Credit Cards Section
        item {
            VaultGlassCard(accentColor = VaultCoral) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Credit Card Outstanding Liabilities", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                    if (creditCards.isEmpty()) {
                        Text("No Credit Card accounts recorded.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                    } else {
                        creditCards.forEach { cc ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cc.account.name, style = MaterialTheme.typography.bodyMedium, color = VaultTextPrimary)
                                Text(
                                    text = InrMoney.formatPaise(cc.closingBalancePaise, isHidden = isHidden),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (cc.closingBalancePaise > 0) VaultCoral else VaultEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // Loans List
        if (uiState.loans.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        text = "No Loans Recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextSecondary
                    )
                }
            }
        } else {
            items(uiState.loans, key = { it.id }) { loan ->
                val calc = remember(loan) {
                    FinancialCalculators.calculateLoanAmortization(
                        principalPaise = loan.remainingPrincipalPaise,
                        annualInterestRatePercent = loan.annualInterestRatePercent,
                        tenureMonths = loan.remainingInstallments.coerceAtLeast(1)
                    )
                }
                val nextInstallment = calc.schedule.firstOrNull()
                val loanTypeLabel = if (loan.loanType == LoanType.OTHER && loan.notes.isNotBlank()) loan.notes else loan.loanType.displayName

                VaultGlassCard(accentColor = VaultGold) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = loan.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VaultTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$loanTypeLabel • ${loan.annualInterestRatePercent}% p.a. • ${loan.remainingInstallments}/${loan.tenureMonths}m left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Remaining Principal",
                                    fontSize = 10.sp,
                                    color = VaultTextMuted,
                                    maxLines = 1
                                )
                                Text(
                                    text = InrMoney.formatPaise(loan.remainingPrincipalPaise, isHidden = isHidden),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VaultCoral,
                                    maxLines = 1
                                )
                            }
                        }

                        HorizontalDivider(color = VaultCardElevated)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly EMI: ${InrMoney.formatPaise(loan.emiAmountPaise, isHidden = isHidden)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VaultGold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectedScheduleLoan = loan }) {
                                    Text("EMI Schedule", style = MaterialTheme.typography.labelSmall)
                                }
                                if (!loan.isClosed && nextInstallment != null && paymentBankAccounts.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            onRecordEmiPayment(
                                                loan.id,
                                                paymentBankAccounts.first().id,
                                                nextInstallment.emiPaise,
                                                nextInstallment.principalComponentPaise,
                                                nextInstallment.interestComponentPaise,
                                                loan.name
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                                    ) {
                                        Text("Pay EMI", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLoanDialog) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        // Start unselected on "Choose" (null) — never pre-select a default!
        var loanType by remember { mutableStateOf<LoanType?>(null) }
        var customLoanTypeLabel by remember { mutableStateOf("") }
        var lender by remember { mutableStateOf("") }
        var principalInput by remember { mutableStateOf("") }
        var rateInput by remember { mutableStateOf("10.5") }
        var tenureInput by remember { mutableStateOf("12") }
        var disbursedAccId by remember { mutableStateOf<Long?>(null) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var highlightMissing by remember { mutableStateOf(false) }
        var typeExpandTrigger by remember { mutableIntStateOf(0) }
        val nameFocusRequester = remember { FocusRequester() }
        val principalFocusRequester = remember { FocusRequester() }

        fun submitLoan() {
            keyboardController?.hide()
            val chosenType = loanType
            if (chosenType == null) {
                highlightMissing = true
                errorMsg = "Please select a Loan Type from the opened menu."
                typeExpandTrigger++
                return
            }
            if (name.isBlank()) {
                highlightMissing = true
                errorMsg = "Please enter a loan name."
                scope.launch {
                    runCatching { nameFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val p = InrMoney.parseInrToPaise(principalInput)
            if (p == null || p <= 0L) {
                highlightMissing = true
                errorMsg = "Please enter a valid principal amount > ₹0."
                scope.launch {
                    runCatching { principalFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val r = rateInput.toDoubleOrNull() ?: 0.0
            val n = tenureInput.toIntOrNull()?.coerceAtLeast(1) ?: 12
            onAddLoan(name, chosenType, lender, p, r, n, IndianFinancialYear.todayEpochDay(), disbursedAccId, customLoanTypeLabel)
            showAddLoanDialog = false
        }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            containerColor = VaultCardSurface,
            title = { Text("Add Loan / Borrowing", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .dismissKeyboardOnSwipeDown()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultChooseSelector(
                        label = "Loan Type",
                        selectedItem = loanType,
                        options = LoanType.entries,
                        onSelect = {
                            loanType = it
                            errorMsg = null
                        },
                        itemLabel = { it.displayName },
                        itemIcon = { loanTypeIcon(it) },
                        placeholder = "Choose",
                        testTag = "loan_type_choose_selector",
                        autoExpandTrigger = typeExpandTrigger,
                        isHighlightedUnselected = highlightMissing && loanType == null,
                        customOptions = uiState.customOptionsMap["LOAN_TYPE"].orEmpty(),
                        customSelectedLabel = customLoanTypeLabel.ifBlank { null },
                        onSelectCustomOption = { customLoanTypeLabel = it },
                        onAddCustomOption = {
                            onAddCustomOption("LOAN_TYPE", it)
                            customLoanTypeLabel = it
                        }
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        isError = highlightMissing && name.isBlank(),
                        label = { Text("Loan Name (Required)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitLoan() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )
                    OutlinedTextField(
                        value = lender,
                        onValueChange = { lender = it },
                        label = { Text("Lender / Bank Name (Optional)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitLoan() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = principalInput,
                        onValueChange = { principalInput = InrMoney.sanitizeIndianNumericInput(it); errorMsg = null },
                        isError = highlightMissing && (InrMoney.parseInrToPaise(principalInput) ?: 0L) <= 0L,
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Loan Principal in INR (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitLoan() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(principalFocusRequester)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it },
                            label = { Text("Interest (% p.a.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tenureInput,
                            onValueChange = { tenureInput = it },
                            label = { Text("Months") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitLoan() }),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val disburseOptions = remember(paymentBankAccounts) {
                        listOf<Long?>(null) + paymentBankAccounts.map { it.id }
                    }
                    VaultChooseSelector(
                        label = "Disbursement Mode",
                        selectedItem = disbursedAccId,
                        options = disburseOptions,
                        onSelect = { disbursedAccId = it },
                        itemLabel = { accId ->
                            if (accId == null) "Existing Loan (Opening Liability)"
                            else "Disbursed to ${paymentBankAccounts.firstOrNull { it.id == accId }?.name ?: "Account"}"
                        },
                        itemIcon = { accId ->
                            val acc = paymentBankAccounts.firstOrNull { it.id == accId }
                            if (acc != null) accountTypeIcon(acc.accountType) else Icons.Default.AccountBalanceWallet
                        },
                        placeholder = "Choose"
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitLoan() },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Loan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLoanDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (selectedScheduleLoan != null) {
        val ln = selectedScheduleLoan!!
        val fullSchedule = remember(ln) {
            FinancialCalculators.calculateLoanAmortization(
                principalPaise = ln.principalPaise,
                annualInterestRatePercent = ln.annualInterestRatePercent,
                tenureMonths = ln.tenureMonths
            )
        }
        AlertDialog(
            onDismissRequest = { selectedScheduleLoan = null },
            containerColor = VaultCardSurface,
            title = { Text("EMI Amortization Schedule: ${ln.name}", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Monthly EMI: ${InrMoney.formatPaise(fullSchedule.monthlyEmiPaise)} | Total Interest: ${InrMoney.formatPaise(fullSchedule.totalInterestPayablePaise)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VaultGold
                    )
                    HorizontalDivider(color = VaultCardElevated)
                    fullSchedule.schedule.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#${item.installmentNumber}", style = MaterialTheme.typography.labelSmall, color = VaultTextMuted)
                            Text("Prin: ${InrMoney.formatPaise(item.principalComponentPaise)}", style = MaterialTheme.typography.labelSmall, color = VaultEmerald)
                            Text("Int: ${InrMoney.formatPaise(item.interestComponentPaise)}", style = MaterialTheme.typography.labelSmall, color = VaultCoral)
                            Text("Bal: ${InrMoney.formatPaise(item.closingPrincipalPaise)}", style = MaterialTheme.typography.labelSmall, color = VaultCyan)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedScheduleLoan = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun BudgetsAndGoalsScreen(
    uiState: FinanceVaultUiState,
    onAddBudget: (String, BudgetPeriod, String, Long, String) -> Unit,
    onSaveOverallBudget: (Long) -> Unit = {},
    onDeleteBudget: (Long) -> Unit,
    onAddGoal: (String, GoalType, Long, Long, String) -> Unit,
    onContributeGoal: (Long, Long) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddCustomOption: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showOverallBudgetDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    val isHidden = uiState.settings.isPrivacyModeEnabled
    val overallStatus = uiState.overallMonthlyBudgetStatus

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_goals_screen"),
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
                        "Budgets & Goals",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showBudgetDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) { Text("+ Budget", maxLines = 1) }
                    Button(
                        onClick = { showGoalDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) { Text("+ Goal", maxLines = 1) }
                }
            }
        }

        // NEW SECTION: Overall Monthly Budget & 80% / 90% Threshold Alert System
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Overall", style = MaterialTheme.typography.titleMedium, color = VaultEmerald)
                TextButton(
                    onClick = { showOverallBudgetDialog = true },
                    modifier = Modifier.testTag("set_overall_budget_button")
                ) {
                    Text(
                        text = if (overallStatus == null) "+ Set Overall Budget" else "Edit Overall",
                        color = VaultCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item {
            val overallAccent = when (overallStatus?.tier) {
                BudgetAlertTier.EXCEEDED_100, BudgetAlertTier.REACHED_90 -> VaultCoral
                BudgetAlertTier.REACHED_80 -> VaultGold
                else -> VaultEmerald
            }
            VaultGlassCard(
                accentColor = overallAccent,
                modifier = Modifier.testTag("overall_budget_card")
            ) {
                if (overallStatus == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Overall Monthly Budget", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                            Text(
                                "Current Month Spent: ${InrMoney.formatPaise(uiState.currentMonthTotalSpentPaise, isHidden = isHidden)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultTextSecondary
                            )
                        }
                        OutlinedButton(onClick = { showOverallBudgetDialog = true }) {
                            Text("Set Limit", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    val pctDisplay = String.format(java.util.Locale.US, "%.1f", overallStatus.usagePercent)
                    val progressFraction = (overallStatus.usagePercent / 100.0).toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Overall Monthly Budget", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                                Text(
                                    "Spent: ${InrMoney.formatPaise(overallStatus.spentPaise, isHidden = isHidden)} of ${InrMoney.formatPaise(overallStatus.limitPaise, isHidden = isHidden)} ($pctDisplay%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = overallAccent
                                )
                            }
                            if (overallStatus.tier != BudgetAlertTier.NORMAL) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(overallAccent.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = overallStatus.tier.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = overallAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteBudget(overallStatus.budget.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Overall Budget", tint = VaultTextMuted)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = overallAccent,
                            trackColor = VaultCardElevated
                        )
                    }
                }
            }
        }

        // Category Budgets with 80% and 90% Threshold Badges
        item {
            Text("Category Budgets (${uiState.settings.activeFinancialYear})", style = MaterialTheme.typography.titleMedium, color = VaultCyan)
        }
        if (uiState.budgetStatuses.isEmpty()) {
            item {
                VaultGlassCard {
                    Text("No category budgets created yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                }
            }
        } else {
            items(uiState.budgetStatuses, key = { it.budget.id }) { status ->
                val b = status.budget
                val pctDisplay = String.format(java.util.Locale.US, "%.1f", status.usagePercent)
                val tierAccent = when (status.tier) {
                    BudgetAlertTier.EXCEEDED_100, BudgetAlertTier.REACHED_90 -> VaultCoral
                    BudgetAlertTier.REACHED_80 -> VaultGold
                    BudgetAlertTier.NORMAL -> VaultEmerald
                }
                val progressFraction = (status.usagePercent / 100.0).toFloat().coerceIn(0f, 1f)
                VaultGlassCard(accentColor = tierAccent) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${b.name} (${b.budgetPeriod.displayName})", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                                Text(
                                    "Category: ${b.category} • Spent: ${InrMoney.formatPaise(status.spentPaise, isHidden = isHidden)} of ${InrMoney.formatPaise(status.limitPaise, isHidden = isHidden)} ($pctDisplay%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tierAccent
                                )
                            }
                            if (status.tier != BudgetAlertTier.NORMAL) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tierAccent.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = status.tier.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tierAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteBudget(b.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultTextMuted)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = tierAccent,
                            trackColor = VaultCardElevated
                        )
                    }
                }
            }
        }

        // Goals
        item {
            Text("Savings & Emergency Fund Goals", style = MaterialTheme.typography.titleMedium, color = VaultEmerald)
        }
        if (uiState.goals.isEmpty()) {
            item {
                VaultGlassCard {
                    Text("No savings goals created yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                }
            }
        } else {
            items(uiState.goals, key = { it.id }) { g ->
                val pct = InrMoney.percentageRatio(g.currentSavedPaise, g.targetAmountPaise)
                VaultGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${g.name} (${g.goalType.displayName})", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                            Text(
                                "Saved: ${InrMoney.formatPaise(g.currentSavedPaise, isHidden = isHidden)} / ${InrMoney.formatPaise(g.targetAmountPaise, isHidden = isHidden)} ($pct%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultCyan
                            )
                        }
                        Row {
                            TextButton(onClick = { onContributeGoal(g.id, 1000_00L) }) {
                                Text("+₹1,000", color = VaultEmerald)
                            }
                            IconButton(onClick = { onDeleteGoal(g.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOverallBudgetDialog) {
        var overallLimitInput by remember(overallStatus) {
            mutableStateOf(
                overallStatus?.limitPaise?.let { InrMoney.paiseToPlainRupeeInput(it) } ?: ""
            )
        }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showOverallBudgetDialog = false },
            containerColor = VaultCardSurface,
            title = { Text("Overall Monthly Budget", color = VaultTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = overallLimitInput,
                        onValueChange = {
                            overallLimitInput = InrMoney.sanitizeIndianNumericInput(it)
                            err = null
                        },
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Monthly Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("overall_budget_limit_input")
                    )
                    if (err != null) {
                        Text(text = err!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = InrMoney.parseInrToPaise(overallLimitInput)
                        if (parsed == null || parsed <= 0L) {
                            err = "Enter a valid monthly limit > ₹0."
                        } else {
                            onSaveOverallBudget(parsed)
                            showOverallBudgetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    modifier = Modifier.testTag("save_overall_budget_button")
                ) {
                    Text("Save Overall Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverallBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBudgetDialog) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf<String?>(null) }
        var limitInput by remember { mutableStateOf("") }
        // Start unselected on "Choose" (null)!
        var period by remember { mutableStateOf<BudgetPeriod?>(null) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var highlightMissing by remember { mutableStateOf(false) }
        var periodExpandTrigger by remember { mutableIntStateOf(0) }
        var catExpandTrigger by remember { mutableIntStateOf(0) }
        val nameFocusRequester = remember { FocusRequester() }
        val limitFocusRequester = remember { FocusRequester() }
        val customCats = uiState.customOptionsMap["TX_CATEGORY"].orEmpty()
        val budgetCategories = remember(customCats) {
            (listOf("Household Expenses", "Utilities & Bills", "Transportation Expenses", "Education Expenses", "Healthcare & Medical") + customCats + "Other").distinct()
        }

        fun submitBudget() {
            keyboardController?.hide()
            val chosenPeriod = period
            if (chosenPeriod == null) {
                highlightMissing = true
                errorMsg = "Please select a Budget Period from the opened menu."
                periodExpandTrigger++
                return
            }
            val chosenCategory = category
            if (chosenCategory == null) {
                highlightMissing = true
                errorMsg = "Please select a Category from the opened menu."
                catExpandTrigger++
                return
            }
            if (name.isBlank()) {
                highlightMissing = true
                errorMsg = "Please enter a budget title."
                scope.launch {
                    runCatching { nameFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val lim = InrMoney.parseInrToPaise(limitInput)
            if (lim == null || lim <= 0L) {
                highlightMissing = true
                errorMsg = "Please enter a valid limit amount > ₹0."
                scope.launch {
                    runCatching { limitFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            onAddBudget(name, chosenPeriod, chosenCategory, lim, "")
            showBudgetDialog = false
        }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            containerColor = VaultCardSurface,
            title = { Text("Create Category Budget", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.dismissKeyboardOnSwipeDown(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultChooseSelector(
                        label = "Budget Period",
                        selectedItem = period,
                        options = BudgetPeriod.entries,
                        onSelect = { period = it; errorMsg = null },
                        itemLabel = { it.displayName },
                        itemIcon = { Icons.Default.CalendarMonth },
                        placeholder = "Choose",
                        testTag = "budget_period_choose_selector",
                        autoExpandTrigger = periodExpandTrigger,
                        isHighlightedUnselected = highlightMissing && period == null
                    )
                    VaultChooseSelector(
                        label = "Category",
                        selectedItem = category,
                        options = budgetCategories,
                        onSelect = { category = it; errorMsg = null },
                        itemLabel = { it },
                        itemIcon = { categoryIcon(it) },
                        placeholder = "Choose",
                        testTag = "budget_category_choose_selector",
                        autoExpandTrigger = catExpandTrigger,
                        isHighlightedUnselected = highlightMissing && category == null,
                        customOptions = customCats,
                        onAddCustomOption = { newCat ->
                            onAddCustomOption("TX_CATEGORY", newCat)
                            category = newCat
                            errorMsg = null
                        }
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        isError = highlightMissing && name.isBlank(),
                        label = { Text("Budget Title") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitBudget() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = InrMoney.sanitizeIndianNumericInput(it); errorMsg = null },
                        isError = highlightMissing && (InrMoney.parseInrToPaise(limitInput) ?: 0L) <= 0L,
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Limit Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitBudget() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(limitFocusRequester)
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { submitBudget() }) { Text("Save Budget") }
            },
            dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") } }
        )
    }

    if (showGoalDialog) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        // Start unselected on "Choose" (null)!
        var goalType by remember { mutableStateOf<GoalType?>(null) }
        var targetInput by remember { mutableStateOf("") }
        var savedInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var highlightMissing by remember { mutableStateOf(false) }
        var typeExpandTrigger by remember { mutableIntStateOf(0) }
        val nameFocusRequester = remember { FocusRequester() }
        val targetFocusRequester = remember { FocusRequester() }

        fun submitGoal() {
            keyboardController?.hide()
            val chosenType = goalType
            if (chosenType == null) {
                highlightMissing = true
                errorMsg = "Please select a Goal Category from the opened menu."
                typeExpandTrigger++
                return
            }
            if (name.isBlank()) {
                highlightMissing = true
                errorMsg = "Please enter a goal name."
                scope.launch {
                    runCatching { nameFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val tgt = InrMoney.parseInrToPaise(targetInput)
            val cur = InrMoney.parseInrToPaise(savedInput) ?: 0L
            if (tgt == null || tgt <= 0L) {
                highlightMissing = true
                errorMsg = "Please enter a valid target amount > ₹0."
                scope.launch {
                    runCatching { targetFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            onAddGoal(name, chosenType, tgt, cur, "")
            showGoalDialog = false
        }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = VaultCardSurface,
            title = { Text("Create Financial Goal", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.dismissKeyboardOnSwipeDown(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultChooseSelector(
                        label = "Goal Category",
                        selectedItem = goalType,
                        options = GoalType.entries,
                        onSelect = { goalType = it; errorMsg = null },
                        itemLabel = { it.displayName },
                        itemIcon = { goalTypeIcon(it) },
                        placeholder = "Choose",
                        testTag = "goal_type_choose_selector",
                        autoExpandTrigger = typeExpandTrigger,
                        isHighlightedUnselected = highlightMissing && goalType == null
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        isError = highlightMissing && name.isBlank(),
                        label = { Text("Goal Name") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGoal() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = InrMoney.sanitizeIndianNumericInput(it); errorMsg = null },
                        isError = highlightMissing && (InrMoney.parseInrToPaise(targetInput) ?: 0L) <= 0L,
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Target Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGoal() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(targetFocusRequester)
                    )
                    OutlinedTextField(
                        value = savedInput,
                        onValueChange = { savedInput = InrMoney.sanitizeIndianNumericInput(it) },
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Already Saved (₹, Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGoal() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { submitGoal() }) { Text("Save Goal") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun GiftCardsScreen(
    uiState: FinanceVaultUiState,
    onAddGiftCard: (String, String, Long, Long, String, String, Long?, String) -> Unit,
    onAddCustomOption: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showAdd by remember { mutableStateOf(false) }
    val isHidden = uiState.settings.isPrivacyModeEnabled

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gift_cards_screen"),
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
                        "Gift Card",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAdd = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gift Card", maxLines = 1)
                }
            }
        }

        if (uiState.giftCards.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        "No Gift Card stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextSecondary
                    )
                }
            }
        } else {
            items(uiState.giftCards, key = { it.id }) { gc ->
                VaultGlassCard(accentColor = VaultGold) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${gc.brand} — ${gc.name}", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                            Text("Card: ${gc.cardNumberMasked} • PIN: ${gc.redemptionCodeProtected}", style = MaterialTheme.typography.labelSmall, color = VaultTextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(InrMoney.formatPaise(gc.remainingValuePaise, isHidden = isHidden), style = MaterialTheme.typography.titleMedium, color = VaultGold)
                            Text("Initial: ${InrMoney.formatPaise(gc.initialValuePaise, isHidden = isHidden)}", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        var brand by remember { mutableStateOf("") }
        var cardCategory by remember { mutableStateOf<String?>(null) }
        var initialInput by remember { mutableStateOf("") }
        var remainingInput by remember { mutableStateOf("") }
        var cardNo by remember { mutableStateOf("") }
        var pinCode by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var highlightMissing by remember { mutableStateOf(false) }
        var catExpandTrigger by remember { mutableIntStateOf(0) }
        val brandFocusRequester = remember { FocusRequester() }
        val initFocusRequester = remember { FocusRequester() }
        val customGcCategories = uiState.customOptionsMap["GIFT_CARD_CATEGORY"].orEmpty()
        val cardCategories = remember(customGcCategories) {
            (listOf("E-Commerce Voucher", "Retail & Apparel", "Dining & Food", "Travel & Hospitality", "Entertainment & Gaming", "Gift Card") + customGcCategories + "Other").distinct()
        }

        fun submitGiftCard() {
            keyboardController?.hide()
            if (cardCategory == null) {
                highlightMissing = true
                errorMsg = "Please select a Gift Card Category from the opened menu."
                catExpandTrigger++
                return
            }
            if (brand.isBlank()) {
                highlightMissing = true
                errorMsg = "Please enter a brand name."
                scope.launch {
                    runCatching { brandFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val initP = InrMoney.parseInrToPaise(initialInput)
            if (initP == null || initP <= 0L) {
                highlightMissing = true
                errorMsg = "Please enter a valid initial value > ₹0."
                scope.launch {
                    runCatching { initFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val remP = InrMoney.parseInrToPaise(remainingInput) ?: initP
            onAddGiftCard(brand, "$brand (${cardCategory!!})", initP, remP, cardNo, pinCode, null, "")
            showAdd = false
        }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = VaultCardSurface,
            title = { Text("Add Gift Card", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.dismissKeyboardOnSwipeDown(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultChooseSelector(
                        label = "Gift Card Category",
                        selectedItem = cardCategory,
                        options = cardCategories,
                        onSelect = { cardCategory = it; errorMsg = null },
                        itemLabel = { it },
                        itemIcon = { Icons.Default.CardGiftcard },
                        placeholder = "Choose",
                        testTag = "gift_card_category_choose_selector",
                        autoExpandTrigger = catExpandTrigger,
                        isHighlightedUnselected = highlightMissing && cardCategory == null,
                        customOptions = customGcCategories,
                        onAddCustomOption = { newCat ->
                            onAddCustomOption("GIFT_CARD_CATEGORY", newCat)
                            cardCategory = newCat
                            errorMsg = null
                        }
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it; errorMsg = null },
                        isError = highlightMissing && brand.isBlank(),
                        label = { Text("Brand Name (Required)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGiftCard() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(brandFocusRequester)
                    )
                    OutlinedTextField(
                        value = initialInput,
                        onValueChange = {
                            val clean = InrMoney.sanitizeIndianNumericInput(it)
                            initialInput = clean
                            if (remainingInput.isBlank()) remainingInput = clean
                            errorMsg = null
                        },
                        isError = highlightMissing && (InrMoney.parseInrToPaise(initialInput) ?: 0L) <= 0L,
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Initial Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGiftCard() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(initFocusRequester)
                    )
                    OutlinedTextField(
                        value = remainingInput,
                        onValueChange = { remainingInput = InrMoney.sanitizeIndianNumericInput(it) },
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Remaining Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGiftCard() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cardNo,
                        onValueChange = { cardNo = it },
                        label = { Text("Card Number (Masked)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGiftCard() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("PIN / Redemption Code (Protected)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitGiftCard() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { submitGiftCard() }) { Text("Save Gift Card") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    uiState: FinanceVaultUiState,
    onAddSubscription: (String, Long, BillingFrequency, Long, Long?, String, Int, String) -> Unit,
    onUpdateRenewalSchedule: (Long, Long, BillingFrequency, Int, String) -> Unit = { _, _, _, _, _ -> },
    onRecordPaymentNow: (Long, Long, Long?) -> Unit,
    onDeleteSubscription: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showAdd by remember { mutableStateOf(false) }
    var selectedRenewSub by remember { mutableStateOf<SubscriptionEntity?>(null) }
    val isHidden = uiState.settings.isPrivacyModeEnabled
    val paymentAccounts = remember(uiState.accountsWithBalances) {
        uiState.accountsWithBalances.map { it.account }.filter { !it.isArchived }
    }

    val totalMonthlyEquivalentPaise = remember(uiState.subscriptions) {
        uiState.subscriptions.sumOf { sub ->
            (sub.costPaise.toDouble() / sub.effectiveMonthsMultiplier()).toLong()
        }
    }
    val totalAnnualEquivalentPaise = totalMonthlyEquivalentPaise * 12L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subscriptions_screen"),
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
                        "Subscriptions",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAdd = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_subscription_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subscription", maxLines = 1)
                }
            }
        }

        item {
            VaultGlassCard(accentColor = VaultCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Monthly Equivalent Cost", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                        Text(
                            if (uiState.subscriptions.isEmpty()) "Not recorded" else InrMoney.formatPaise(totalMonthlyEquivalentPaise, isHidden = isHidden),
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultCyan
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Annual Equivalent Cost", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                        Text(
                            if (uiState.subscriptions.isEmpty()) "Not recorded" else InrMoney.formatPaise(totalAnnualEquivalentPaise, isHidden = isHidden),
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultGold
                        )
                    }
                }
            }
        }

        if (uiState.subscriptions.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        "No subscriptions recorded. Tap + Subscription to add monthly, 2-month, 4-month, or custom renewal dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextSecondary
                    )
                }
            }
        } else {
            items(uiState.subscriptions, key = { it.id }) { sub ->
                val today = IndianFinancialYear.todayEpochDay()
                val daysRemaining = sub.nextRenewalEpochDay - today
                val dueBadgeText = when {
                    daysRemaining < 0 -> "Overdue by ${-daysRemaining}d"
                    daysRemaining == 0L -> "Due Today"
                    else -> "In ${daysRemaining}d"
                }
                val dueBadgeColor = when {
                    daysRemaining <= 0L -> VaultCoral
                    daysRemaining <= 7L -> VaultGold
                    else -> VaultCyan
                }

                VaultGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sub.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VaultTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${InrMoney.formatPaise(sub.costPaise, isHidden = isHidden)} • ${sub.cycleLabel()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultEmerald
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = dueBadgeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Next Renewal: ${IndianFinancialYear.formatEpochDay(sub.nextRenewalEpochDay)} ($dueBadgeText)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = dueBadgeColor
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteSubscription(sub.id) },
                                modifier = Modifier.testTag("delete_subscription_${sub.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultTextMuted)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedRenewSub = sub },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_renewal_date_button_${sub.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = VaultCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Custom Renewal Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultCyan,
                                    maxLines = 1
                                )
                            }

                            if (paymentAccounts.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onRecordPaymentNow(
                                            sub.id,
                                            sub.paymentAccountId ?: paymentAccounts.first().id,
                                            null
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("record_sub_payment_button_${sub.id}")
                                ) {
                                    Text(
                                        "Record Payment",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultObsidianBg,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Renewal Date & Billing Cycle Window for an existing Subscription
    if (selectedRenewSub != null) {
        val sub = selectedRenewSub!!
        var editFreq by remember(sub) { mutableStateOf(sub.billingFrequency) }
        var customCountInput by remember(sub) {
            mutableStateOf(
                when (sub.billingFrequency) {
                    BillingFrequency.BIMONTHLY -> "2"
                    BillingFrequency.FOUR_MONTHLY -> "4"
                    else -> sub.customIntervalCount.coerceAtLeast(1).toString()
                }
            )
        }
        var customUnit by remember(sub) { mutableStateOf(sub.customIntervalUnit.ifBlank { "MONTHS" }) }
        var customDateInput by remember(sub) {
            mutableStateOf(IndianFinancialYear.formatShortEpochDay(sub.nextRenewalEpochDay))
        }
        var showCalendarPicker by remember { mutableStateOf(false) }
        var selectedPayAccId by remember(sub, paymentAccounts) {
            mutableStateOf(sub.paymentAccountId ?: paymentAccounts.firstOrNull()?.id)
        }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        fun applyQuickCyclePreset(months: Int, targetFreq: BillingFrequency) {
            editFreq = targetFreq
            customCountInput = months.toString()
            customUnit = "MONTHS"
            val nextEpoch = IndianFinancialYear.addMonthsToEpochDay(IndianFinancialYear.todayEpochDay(), months.toLong())
            customDateInput = IndianFinancialYear.formatShortEpochDay(nextEpoch)
            errorMsg = null
        }

        if (showCalendarPicker) {
            val currentParsedEpoch = IndianFinancialYear.parseFlexibleDateInput(customDateInput) ?: sub.nextRenewalEpochDay
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = currentParsedEpoch * 86_400_000L
            )
            DatePickerDialog(
                onDismissRequest = { showCalendarPicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                val chosenEpochDay = millis / 86_400_000L
                                customDateInput = IndianFinancialYear.formatShortEpochDay(chosenEpochDay)
                            }
                            showCalendarPicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                    ) {
                        Text("Select Date", color = VaultObsidianBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendarPicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { selectedRenewSub = null },
            containerColor = VaultCardSurface,
            title = {
                Text(
                    "Subscription Renewal & Custom Date: ${sub.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick Multi-Month Renewal Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Triple("Every 1 Month", 1, BillingFrequency.MONTHLY),
                            Triple("Every 2 Months", 2, BillingFrequency.BIMONTHLY),
                            Triple("Every 3 Months", 3, BillingFrequency.QUARTERLY),
                            Triple("Every 4 Months", 4, BillingFrequency.FOUR_MONTHLY),
                            Triple("Every 6 Months", 6, BillingFrequency.HALF_YEARLY),
                            Triple("Every 12 Months", 12, BillingFrequency.ANNUALLY),
                            Triple("Custom Interval", customCountInput.toIntOrNull() ?: 2, BillingFrequency.CUSTOM)
                        )
                        presets.forEach { (label, mCount, presetFreq) ->
                            val isSelected = editFreq == presetFreq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) VaultEmerald.copy(alpha = 0.22f) else VaultObsidianBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) VaultEmerald else VaultCyan.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        applyQuickCyclePreset(mCount, presetFreq)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) VaultEmerald else VaultTextPrimary
                                )
                            }
                        }
                    }

                    if (editFreq == BillingFrequency.CUSTOM || editFreq == BillingFrequency.BIMONTHLY || editFreq == BillingFrequency.FOUR_MONTHLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customCountInput,
                                onValueChange = {
                                    customCountInput = it.filter { c -> c.isDigit() }.take(3)
                                    editFreq = BillingFrequency.CUSTOM
                                    val n = customCountInput.toLongOrNull() ?: 1L
                                    if (n > 0) {
                                        val nextDay = if (customUnit == "DAYS") {
                                            IndianFinancialYear.addDaysToEpochDay(IndianFinancialYear.todayEpochDay(), n)
                                        } else {
                                            IndianFinancialYear.addMonthsToEpochDay(IndianFinancialYear.todayEpochDay(), n)
                                        }
                                        customDateInput = IndianFinancialYear.formatShortEpochDay(nextDay)
                                    }
                                },
                                label = { Text("Repeat Every") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("renew_custom_interval_count_input")
                            )
                            VaultOptionTogglePill(
                                selected = customUnit == "MONTHS",
                                label = "Months",
                                onClick = {
                                    customUnit = "MONTHS"
                                    val n = customCountInput.toLongOrNull() ?: 1L
                                    val nextDay = IndianFinancialYear.addMonthsToEpochDay(IndianFinancialYear.todayEpochDay(), n)
                                    customDateInput = IndianFinancialYear.formatShortEpochDay(nextDay)
                                }
                            )
                            VaultOptionTogglePill(
                                selected = customUnit == "DAYS",
                                label = "Days",
                                onClick = {
                                    customUnit = "DAYS"
                                    val n = customCountInput.toLongOrNull() ?: 30L
                                    val nextDay = IndianFinancialYear.addDaysToEpochDay(IndianFinancialYear.todayEpochDay(), n)
                                    customDateInput = IndianFinancialYear.formatShortEpochDay(nextDay)
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customDateInput,
                        onValueChange = {
                            customDateInput = it
                            errorMsg = null
                        },
                        label = { Text("Custom Next Renewal Date (DD/MM/YYYY)") },
                        placeholder = { Text("DD/MM/YYYY") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showCalendarPicker = true },
                                modifier = Modifier.testTag("open_renewal_calendar_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Pick Custom Date",
                                    tint = VaultCyan
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_renewal_date_input")
                    )

                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (paymentAccounts.isNotEmpty() && selectedPayAccId != null) {
                        OutlinedButton(
                            onClick = {
                                val parsedEpoch = IndianFinancialYear.parseFlexibleDateInput(customDateInput)
                                if (parsedEpoch == null) {
                                    errorMsg = "Please enter a valid date in DD/MM/YYYY format or use the calendar icon."
                                    return@OutlinedButton
                                }
                                val count = customCountInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                onUpdateRenewalSchedule(sub.id, parsedEpoch, editFreq, count, customUnit)
                                onRecordPaymentNow(sub.id, selectedPayAccId!!, parsedEpoch)
                                selectedRenewSub = null
                            },
                            modifier = Modifier.testTag("pay_and_save_custom_renewal_button")
                        ) {
                            Text("Pay & Set Date", color = VaultCyan)
                        }
                    }

                    Button(
                        onClick = {
                            val parsedEpoch = IndianFinancialYear.parseFlexibleDateInput(customDateInput)
                            if (parsedEpoch == null) {
                                errorMsg = "Please enter a valid date in DD/MM/YYYY format or use the calendar icon."
                                return@Button
                            }
                            val count = customCountInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            onUpdateRenewalSchedule(sub.id, parsedEpoch, editFreq, count, customUnit)
                            selectedRenewSub = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                        modifier = Modifier.testTag("save_custom_renewal_date_button")
                    ) {
                        Text("Save Date", color = VaultObsidianBg, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRenewSub = null }) { Text("Cancel") }
            }
        )
    }

    if (showAdd) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var costInput by remember { mutableStateOf("") }
        var freq by remember { mutableStateOf<BillingFrequency?>(null) }
        var customIntervalCountInput by remember { mutableStateOf("2") }
        var customIntervalUnit by remember { mutableStateOf("MONTHS") }
        var customRenewalDateInput by remember {
            mutableStateOf(IndianFinancialYear.formatShortEpochDay(IndianFinancialYear.addMonthsToEpochDay(IndianFinancialYear.todayEpochDay(), 1)))
        }
        var showAddDatePicker by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var highlightMissing by remember { mutableStateOf(false) }
        var freqExpandTrigger by remember { mutableIntStateOf(0) }
        val nameFocusRequester = remember { FocusRequester() }
        val costFocusRequester = remember { FocusRequester() }

        fun syncDefaultRenewalDateForFrequency(chosen: BillingFrequency, countStr: String, unit: String) {
            val today = IndianFinancialYear.todayEpochDay()
            val count = countStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val targetEpoch = IndianFinancialYear.advanceSubscriptionRenewalEpochDay(
                fromEpochDay = today,
                frequency = chosen,
                customIntervalCount = count,
                customIntervalUnit = unit
            )
            customRenewalDateInput = IndianFinancialYear.formatShortEpochDay(targetEpoch)
        }

        if (showAddDatePicker) {
            val initialEpoch = IndianFinancialYear.parseFlexibleDateInput(customRenewalDateInput)
                ?: (IndianFinancialYear.todayEpochDay() + 30L)
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialEpoch * 86_400_000L
            )
            DatePickerDialog(
                onDismissRequest = { showAddDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                customRenewalDateInput = IndianFinancialYear.formatShortEpochDay(millis / 86_400_000L)
                            }
                            showAddDatePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                    ) {
                        Text("Select Date", color = VaultObsidianBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        fun submitSubscription() {
            keyboardController?.hide()
            val chosenFreq = freq
            if (chosenFreq == null) {
                highlightMissing = true
                errorMsg = "Please select a Billing Frequency from the opened menu."
                freqExpandTrigger++
                return
            }
            if (name.isBlank()) {
                highlightMissing = true
                errorMsg = "Please enter a subscription name."
                scope.launch {
                    runCatching { nameFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val c = InrMoney.parseInrToPaise(costInput)
            if (c == null || c <= 0L) {
                highlightMissing = true
                errorMsg = "Please enter a valid cost > ₹0."
                scope.launch {
                    runCatching { costFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            val parsedEpoch = IndianFinancialYear.parseFlexibleDateInput(customRenewalDateInput)
            if (parsedEpoch == null) {
                highlightMissing = true
                errorMsg = "Please enter a valid custom renewal date (DD/MM/YYYY) or tap the calendar icon."
                return
            }
            val finalIntervalCount = when (chosenFreq) {
                BillingFrequency.BIMONTHLY -> 2
                BillingFrequency.FOUR_MONTHLY -> 4
                BillingFrequency.CUSTOM -> customIntervalCountInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                else -> chosenFreq.monthsMultiplier.toInt().coerceAtLeast(1)
            }
            onAddSubscription(
                name,
                c,
                chosenFreq,
                parsedEpoch,
                paymentAccounts.firstOrNull()?.id,
                "",
                finalIntervalCount,
                customIntervalUnit
            )
            showAdd = false
        }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = VaultCardSurface,
            title = { Text("Add Subscription & Custom Renewal Date", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .dismissKeyboardOnSwipeDown()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultChooseSelector(
                        label = "Billing Frequency",
                        selectedItem = freq,
                        options = BillingFrequency.entries,
                        onSelect = {
                            freq = it
                            errorMsg = null
                            if (it == BillingFrequency.BIMONTHLY) customIntervalCountInput = "2"
                            if (it == BillingFrequency.FOUR_MONTHLY) customIntervalCountInput = "4"
                            syncDefaultRenewalDateForFrequency(it, customIntervalCountInput, customIntervalUnit)
                        },
                        itemLabel = { it.displayName },
                        itemIcon = { Icons.Default.Autorenew },
                        placeholder = "Choose",
                        testTag = "subscription_freq_choose_selector",
                        autoExpandTrigger = freqExpandTrigger,
                        isHighlightedUnselected = highlightMissing && freq == null
                    )

                    if (freq == BillingFrequency.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customIntervalCountInput,
                                onValueChange = {
                                    customIntervalCountInput = it.filter { c -> c.isDigit() }.take(3)
                                    syncDefaultRenewalDateForFrequency(
                                        BillingFrequency.CUSTOM,
                                        customIntervalCountInput,
                                        customIntervalUnit
                                    )
                                },
                                label = { Text("Every (e.g. 2, 4)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_sub_custom_interval_count")
                            )
                            VaultOptionTogglePill(
                                selected = customIntervalUnit == "MONTHS",
                                label = "Months",
                                onClick = {
                                    customIntervalUnit = "MONTHS"
                                    syncDefaultRenewalDateForFrequency(
                                        BillingFrequency.CUSTOM,
                                        customIntervalCountInput,
                                        "MONTHS"
                                    )
                                }
                            )
                            VaultOptionTogglePill(
                                selected = customIntervalUnit == "DAYS",
                                label = "Days",
                                onClick = {
                                    customIntervalUnit = "DAYS"
                                    syncDefaultRenewalDateForFrequency(
                                        BillingFrequency.CUSTOM,
                                        customIntervalCountInput,
                                        "DAYS"
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        isError = highlightMissing && name.isBlank(),
                        label = { Text("Subscription Name") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitSubscription() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = InrMoney.sanitizeIndianNumericInput(it); errorMsg = null },
                        isError = highlightMissing && (InrMoney.parseInrToPaise(costInput) ?: 0L) <= 0L,
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Renewal Cost in INR (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitSubscription() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(costFocusRequester)
                    )

                    OutlinedTextField(
                        value = customRenewalDateInput,
                        onValueChange = {
                            customRenewalDateInput = it
                            errorMsg = null
                        },
                        label = { Text("Custom Next Renewal Date (DD/MM/YYYY)") },
                        placeholder = { Text("DD/MM/YYYY") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showAddDatePicker = true },
                                modifier = Modifier.testTag("add_sub_calendar_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Pick Custom Renewal Date",
                                    tint = VaultCyan
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_sub_custom_renewal_date_input")
                    )

                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { submitSubscription() }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TaxesScreen(
    uiState: FinanceVaultUiState,
    onAddTaxRecord: (TaxType, String, Long, String, Long?, String, String) -> Unit,
    onAddCustomOption: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showAdd by remember { mutableStateOf(false) }
    val isHidden = uiState.settings.isPrivacyModeEnabled
    val bankAccounts = remember(uiState.accountsWithBalances) {
        uiState.accountsWithBalances.map { it.account }.filter { !it.isArchived }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("taxes_screen"),
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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Indian Tax Records",
                            style = MaterialTheme.typography.titleLarge,
                            color = VaultTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAdd = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("record_tax_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Record Tax",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }

        item {
            VaultGlassCard(accentColor = VaultGold) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val totalPaid = uiState.taxRecords.sumOf { it.amountPaidPaise }
                    val fy = uiState.settings.activeFinancialYear
                    val ay = IndianFinancialYear.getAssessmentYearForFy(fy)
                    Text("Active Period: $fy ($ay)", style = MaterialTheme.typography.labelLarge, color = VaultGold)
                    Text(
                        "Total Recorded Tax Payments: ${if (uiState.taxRecords.isEmpty()) "Not recorded" else InrMoney.formatPaise(totalPaid, isHidden = isHidden)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Do not use for official purposes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextMuted,
                        modifier = Modifier.testTag("tax_short_disclaimer")
                    )
                }
            }
        }

        items(uiState.taxRecords, key = { it.id }) { rec ->
            val taxLabel = rec.supportingDocLabel.ifBlank { rec.taxType.displayName }
            VaultGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$taxLabel (${rec.financialYear} / ${rec.assessmentYear})", style = MaterialTheme.typography.titleSmall, color = VaultTextPrimary)
                        Text(
                            "Challan/Ref: ${rec.challanNumber.ifBlank { "Not provided" }} • Date: ${IndianFinancialYear.formatEpochDay(rec.paymentDateEpochDay)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextSecondary
                        )
                    }
                    Text(
                        InrMoney.formatPaise(rec.amountPaidPaise, isHidden = isHidden),
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultGold
                    )
                }
            }
        }
    }

    if (showAdd) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        // Start unselected on "Choose" (null)!
        var taxType by remember { mutableStateOf<TaxType?>(null) }
        var customTaxLabel by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var challan by remember { mutableStateOf("") }
        var selectedAccId by remember { mutableStateOf<Long?>(null) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var highlightMissing by remember { mutableStateOf(false) }
        var typeExpandTrigger by remember { mutableIntStateOf(0) }
        val amountFocusRequester = remember { FocusRequester() }
        val customTaxCategories = uiState.customOptionsMap["TAX_CATEGORY"].orEmpty()

        fun submitTax() {
            keyboardController?.hide()
            val chosenTaxType = taxType
            if (chosenTaxType == null) {
                highlightMissing = true
                errorMsg = "Please select a Tax Category from the opened menu."
                typeExpandTrigger++
                return
            }
            val amt = InrMoney.parseInrToPaise(amountInput)
            if (amt == null || amt <= 0L) {
                highlightMissing = true
                errorMsg = "Please enter a valid tax amount > ₹0."
                scope.launch {
                    runCatching { amountFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
                return
            }
            onAddTaxRecord(chosenTaxType, uiState.settings.activeFinancialYear, amt, challan, selectedAccId, customTaxLabel, "")
            showAdd = false
        }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = VaultCardSurface,
            title = { Text("Record Tax Payment", color = VaultTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.dismissKeyboardOnSwipeDown(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultChooseSelector(
                        label = "Tax Category",
                        selectedItem = taxType,
                        options = TaxType.entries,
                        onSelect = { taxType = it; errorMsg = null },
                        itemLabel = { it.displayName },
                        itemIcon = { taxTypeIcon(it) },
                        placeholder = "Choose",
                        testTag = "tax_type_choose_selector",
                        autoExpandTrigger = typeExpandTrigger,
                        isHighlightedUnselected = highlightMissing && taxType == null,
                        customOptions = customTaxCategories,
                        customSelectedLabel = customTaxLabel.ifBlank { null },
                        onSelectCustomOption = { customTaxLabel = it },
                        onAddCustomOption = { newTaxCat ->
                            onAddCustomOption("TAX_CATEGORY", newTaxCat)
                            customTaxLabel = newTaxCat
                            errorMsg = null
                        }
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = InrMoney.sanitizeIndianNumericInput(it); errorMsg = null },
                        isError = highlightMissing && (InrMoney.parseInrToPaise(amountInput) ?: 0L) <= 0L,
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Tax Amount Paid in INR (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTax() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(amountFocusRequester)
                    )
                    OutlinedTextField(
                        value = challan,
                        onValueChange = { challan = it },
                        label = { Text("BSR / Challan / Form 26AS Ref (Optional)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTax() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val bankOptions = remember(bankAccounts) {
                        listOf<Long?>(null) + bankAccounts.map { it.id }
                    }
                    VaultChooseSelector(
                        label = "Deduct from Bank Account (Optional)",
                        selectedItem = selectedAccId,
                        options = bankOptions,
                        onSelect = { selectedAccId = it },
                        itemLabel = { accId ->
                            if (accId == null) "Record Only (Already Deducted)"
                            else bankAccounts.firstOrNull { it.id == accId }?.name ?: "Bank Account"
                        },
                        itemIcon = { accId ->
                            val acc = bankAccounts.firstOrNull { it.id == accId }
                            if (acc != null) accountTypeIcon(acc.accountType) else Icons.Default.AccountBalanceWallet
                        },
                        placeholder = "Choose"
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { submitTax() }) { Text("Save Tax Record") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SettingsAndAboutScreen(
    uiState: FinanceVaultUiState,
    onUpdateProfileAndFy: (String, String, String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdatePin: (String?) -> Unit,
    onVerifyOldPin: (String) -> Boolean = { true },
    onToggleBiometric: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onExportJsonBackup: () -> Unit,
    onRestoreJsonBackup: (String) -> Unit,
    onClearData: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var userName by remember(uiState.settings.userName) { mutableStateOf(uiState.settings.userName) }
    var panMasked by remember(uiState.settings.panMasked) { mutableStateOf(uiState.settings.panMasked) }
    val autoSystemFy = remember { IndianFinancialYear.currentFyLabel() }
    val hasExistingPin = !uiState.settings.pinHashSha256.isNullOrBlank()
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var pinValidationError by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAuditTrailDialog by remember { mutableStateOf(false) }
    val biometricAvailability = remember(context) { BiometricAuthHelper.checkBiometricAvailability(context) }

    if (showAuditTrailDialog) {
        AuditTrailFullDialog(
            auditLogs = uiState.auditLogs,
            onDismiss = { showAuditTrailDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                }
                Column {
                    Text("Settings, Security & About", style = MaterialTheme.typography.titleLarge, color = VaultTextPrimary)
                }
            }
        }

        // About & Creator Credit Card
        item {
            VaultGlassCard(accentColor = VaultEmerald) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Finance Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Made by Lenin and Shaswat",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultEmerald,
                        modifier = Modifier.testTag("about_creator_credit")
                    )
                }
            }
        }

        // Appearance Selector (System, Light Mode, Dark Mode)
        item {
            VaultGlassCard(accentColor = VaultCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val orderedModes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                        orderedModes.forEach { mode ->
                            val icon = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            }
                            VaultOptionTogglePill(
                                selected = uiState.settings.themeMode == mode,
                                label = mode.displayName,
                                leadingIcon = icon,
                                onClick = { onUpdateThemeMode(mode) },
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Personal Details & Financial Year
        item {
            VaultGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personal Details", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Not provided") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Financial Year: $autoSystemFy (${IndianFinancialYear.getAssessmentYearForFy(autoSystemFy)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultEmerald,
                        modifier = Modifier.testTag("auto_system_fy_label")
                    )
                    Button(
                        onClick = { onUpdateProfileAndFy(userName, panMasked, autoSystemFy) },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        // 6-Hour & Custom Multi-Period Transaction Recall Reminders
        item {
            val reminderState by TransactionRecallReminderManager.state.collectAsStateWithLifecycle()
            CustomRecallRemindersManagementCard(
                reminderState = reminderState
            )
        }

        // Privacy & Demonstration Mode
        item {
            VaultGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Privacy & Demonstration Mode", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hidden Balance Privacy Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VaultTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        VaultToggleSwitch(
                            checked = uiState.settings.isPrivacyModeEnabled,
                            onCheckedChange = { onTogglePrivacy() },
                            modifier = Modifier.testTag("settings_privacy_mode_switch")
                        )
                    }
                    HorizontalDivider(color = VaultCardElevated)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Fictional Demonstration Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VaultTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        VaultToggleSwitch(
                            checked = uiState.settings.isDemoModeEnabled,
                            onCheckedChange = { onToggleDemoMode(it) },
                            modifier = Modifier.testTag("settings_demo_mode_switch")
                        )
                    }
                }
            }
        }

        // Security (Double confirmation when adding a new password/PIN; Old + New + Confirm when changing)
        item {
            VaultGlassCard(accentColor = VaultCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = VaultCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                    }
                    Text(
                        text = if (!hasExistingPin) "Password / PIN Lock: Not configured"
                        else "Password / PIN Lock: Enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultEmerald
                    )

                    if (hasExistingPin) {
                        OutlinedTextField(
                            value = oldPinInput,
                            onValueChange = {
                                oldPinInput = it.filter { c -> c.isDigit() }.take(8)
                                pinValidationError = null
                            },
                            label = { Text("Old Password / PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_old_pin_input")
                        )
                    }

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            newPinInput = it.filter { c -> c.isDigit() }.take(8)
                            pinValidationError = null
                        },
                        label = { Text("New Password / PIN (4–8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_new_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = {
                            confirmNewPinInput = it.filter { c -> c.isDigit() }.take(8)
                            pinValidationError = null
                        },
                        label = { Text("Confirm New Password / PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_confirm_pin_input")
                    )

                    if (pinValidationError != null) {
                        Text(
                            text = pinValidationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultCoral
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (hasExistingPin) {
                                    if (oldPinInput.isBlank() || !onVerifyOldPin(oldPinInput)) {
                                        pinValidationError = "Incorrect old password / PIN."
                                        return@Button
                                    }
                                }
                                if (newPinInput.length < 4) {
                                    pinValidationError = "Password / PIN must be at least 4 digits."
                                    return@Button
                                }
                                if (newPinInput != confirmNewPinInput) {
                                    pinValidationError = "New password / PIN entries do not match."
                                    return@Button
                                }
                                onUpdatePin(newPinInput)
                                oldPinInput = ""
                                newPinInput = ""
                                confirmNewPinInput = ""
                                pinValidationError = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                            modifier = Modifier.testTag("security_save_pin_button")
                        ) {
                            Text(
                                text = if (hasExistingPin) "Change Password / PIN" else "Set Password / PIN",
                                color = VaultObsidianBg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (hasExistingPin) {
                            TextButton(
                                onClick = {
                                    if (oldPinInput.isBlank() || !onVerifyOldPin(oldPinInput)) {
                                        pinValidationError = "Enter your current password / PIN above to remove it."
                                    } else {
                                        onUpdatePin(null)
                                        oldPinInput = ""
                                        newPinInput = ""
                                        confirmNewPinInput = ""
                                        pinValidationError = null
                                    }
                                }
                            ) {
                                Text("Remove PIN", color = VaultCoral)
                            }
                        }
                    }

                    HorizontalDivider(color = VaultCardElevated)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = VaultEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Biometric Unlock",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VaultTextPrimary
                            )
                        }
                        VaultToggleSwitch(
                            checked = uiState.settings.isBiometricEnabled,
                            onCheckedChange = { onToggleBiometric(it) },
                            enabled = hasExistingPin,
                            modifier = Modifier.testTag("settings_biometric_switch")
                        )
                    }
                }
            }
        }

        // Offline Backup, Restore & Data Deletion
        item {
            VaultGlassCard(accentColor = VaultGold) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup, Restore & Data Control", style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onExportJsonBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                            modifier = Modifier.testTag("export_json_backup_button")
                        ) {
                            Text("Export JSON Backup")
                        }
                        OutlinedButton(onClick = { showRestoreDialog = true }) {
                            Text("Restore Backup", color = VaultCyan)
                        }
                    }
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("delete_all_data_button")
                    ) {
                        Text(
                            if (uiState.settings.isDemoModeEnabled) "Reset Fictional Demo Records"
                            else "Delete All Real Financial Records",
                            color = VaultCoral
                        )
                    }
                }
            }
        }

        // Interactive Account Audit Trail Card (Tap to view all events)
        item {
            VaultGlassCard(
                accentColor = VaultEmerald,
                onClick = { showAuditTrailDialog = true },
                modifier = Modifier.testTag("settings_audit_trail_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Account Audit Trail (${uiState.auditLogs.size} events)",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View All", style = MaterialTheme.typography.labelMedium, color = VaultEmerald)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "View All Audit Events",
                                tint = VaultEmerald
                            )
                        }
                    }
                    if (uiState.auditLogs.isEmpty()) {
                        Text("No audit operations recorded yet.", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                    } else {
                        uiState.auditLogs.take(5).forEach { log ->
                            Text(
                                text = "• [${log.action}] ${log.summary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultTextSecondary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = VaultCardSurface,
            title = { Text("Restore from JSON Backup", color = VaultTextPrimary) },
            text = {
                OutlinedTextField(
                    value = restoreJsonInput,
                    onValueChange = { restoreJsonInput = it },
                    label = { Text("Paste Finance Vault JSON Backup") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (restoreJsonInput.isNotBlank()) {
                        onRestoreJsonBackup(restoreJsonInput)
                        showRestoreDialog = false
                    }
                }) { Text("Confirm Restore") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") } }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = VaultCardSurface,
            title = { Text("Confirm Data Deletion", color = VaultCoral) },
            text = {
                Text(
                    "Are you sure you want to delete all records in the current mode?",
                    color = VaultTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCoral)
                ) { Text("Delete Permanently") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AuditTrailFullDialog(
    auditLogs: List<AuditLogEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Text(
                text = "Account Audit Trail (${auditLogs.size} events)",
                style = MaterialTheme.typography.titleLarge,
                color = VaultTextPrimary
            )
        },
        text = {
            if (auditLogs.isEmpty()) {
                Text(
                    text = "No money transactions recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VaultTextSecondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .testTag("audit_trail_dialog_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs, key = { it.id }) { log ->
                        val dateLabel = IndianFinancialYear.formatEpochDay(log.timestampMillis / 86_400_000L)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultObsidianBg)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${log.action} • ${log.entityType}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VaultEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextMuted
                                )
                            }
                            Text(
                                text = log.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultTextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                modifier = Modifier.testTag("close_audit_trail_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}
