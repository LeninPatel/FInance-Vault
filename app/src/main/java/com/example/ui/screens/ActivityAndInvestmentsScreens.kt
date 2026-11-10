package com.example.ui.screens

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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import com.example.data.local.AccountType
import com.example.data.local.CompoundingFrequency
import com.example.data.local.InvestmentCategory
import com.example.data.local.InvestmentEntity
import com.example.data.local.TransactionType
import com.example.domain.calculators.FinancialCalculators
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.ui.AccountWithBalance
import com.example.ui.FinanceVaultUiState
import com.example.ui.components.IndianCurrencyVisualTransformation
import com.example.ui.components.VaultChooseSelector
import com.example.ui.components.VaultGlassCard
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.dismissKeyboardOnSwipeDown
import com.example.ui.components.investmentCategoryIcon
import com.example.ui.components.transactionTypeIcon
import com.example.ui.theme.VaultCardElevated
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun ActivityScreen(
    uiState: FinanceVaultUiState,
    searchQuery: String = "",
    onOpenAddTransaction: () -> Unit,
    onReverseTransaction: (Long) -> Unit,
    onBulkReverseTransactions: (Set<Long>) -> Unit = {},
    onBulkCategorizeTransactions: (Set<Long>, String) -> Unit = { _, _ -> },
    onAddCustomOption: (String, String) -> Unit = { _, _ -> }
) {
    // Starts unselected on "Choose" (null) — shows all transactions until a filter is chosen
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    val isHidden = uiState.settings.isPrivacyModeEnabled

    val filteredTransactions = remember(uiState.transactions, searchQuery, selectedTypeFilter) {
        if (selectedTypeFilter == null && searchQuery.isBlank()) {
            uiState.transactions
        } else {
            uiState.transactions.filter { tx ->
                val matchesType = selectedTypeFilter == null || tx.transactionType == selectedTypeFilter
                val matchesQuery = searchQuery.isBlank() || tx.matchesSearchQuery(searchQuery)
                matchesType && matchesQuery
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val dragCoords = remember { FloatArray(4) } // [0]=listTopY, [1]=listBottomY, [2]=dragStartY, [3]=dragCurrentY
    var selectedTxIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragInitialIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragAnchorTxId by remember { mutableStateOf<Long?>(null) }
    var isFingerPressedSelecting by remember { mutableStateOf(false) }
    var showBulkCategorizeDialog by remember { mutableStateOf(false) }
    var pendingDeleteTxIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val itemBoundsInRoot = remember { mutableMapOf<Long, ClosedFloatingPointRange<Float>>() }
    val isBatchSelectionMode by remember { derivedStateOf { selectedTxIds.isNotEmpty() } }

    fun updateSelectionForRange(startY: Float, currentY: Float) {
        val minY = min(startY, currentY)
        val maxY = max(startY, currentY)
        val swipedIds = itemBoundsInRoot.filter { (_, bounds) ->
            bounds.endInclusive >= minY && bounds.start <= maxY
        }.keys
        val anchorId = dragAnchorTxId
        val indexRangeIds = if (anchorId != null && swipedIds.isNotEmpty()) {
            val anchorIdx = filteredTransactions.indexOfFirst { it.id == anchorId }
            val hitIndices = filteredTransactions.mapIndexedNotNull { idx, item ->
                if (item.id in swipedIds) idx else null
            }
            if (anchorIdx >= 0 && hitIndices.isNotEmpty()) {
                val minIdx = min(anchorIdx, hitIndices.minOrNull() ?: anchorIdx)
                val maxIdx = max(anchorIdx, hitIndices.maxOrNull() ?: anchorIdx)
                filteredTransactions.subList(minIdx, maxIdx + 1).map { it.id }.toSet()
            } else emptySet()
        } else emptySet()
        val nextIds = dragInitialIds + swipedIds + indexRangeIds
        if (nextIds != selectedTxIds) {
            selectedTxIds = nextIds
        }
    }

    LaunchedEffect(isFingerPressedSelecting) {
        while (isFingerPressedSelecting) {
            val listTopRootY = dragCoords[0]
            val listBottomRootY = dragCoords[1]
            val activeDragStartY = dragCoords[2]
            val activeDragCurrentY = dragCoords[3]
            val edgeZone = 140f
            val scrollDelta = when {
                listBottomRootY > listTopRootY && activeDragCurrentY > (listBottomRootY - edgeZone) -> {
                    ((activeDragCurrentY - (listBottomRootY - edgeZone)).coerceIn(10f, 160f)) * 0.35f
                }
                listBottomRootY > listTopRootY && activeDragCurrentY < (listTopRootY + edgeZone) -> {
                    -((listTopRootY + edgeZone - activeDragCurrentY).coerceIn(10f, 160f)) * 0.35f
                }
                else -> 0f
            }
            if (scrollDelta != 0f) {
                val consumed = lazyListState.scrollBy(scrollDelta)
                if (consumed != 0f) {
                    updateSelectionForRange(activeDragStartY, activeDragCurrentY)
                }
            }
            delay(16L)
        }
    }

    if (pendingDeleteTxIds.isNotEmpty()) {
        ConfirmDeleteTransactionsDialog(
            deleteCount = pendingDeleteTxIds.size,
            onDismiss = { pendingDeleteTxIds = emptySet() },
            onConfirmDelete = {
                val idsToDelete = pendingDeleteTxIds
                pendingDeleteTxIds = emptySet()
                if (idsToDelete.size == 1 && selectedTxIds.isEmpty()) {
                    onReverseTransaction(idsToDelete.first())
                } else {
                    onBulkReverseTransactions(idsToDelete)
                    selectedTxIds = selectedTxIds - idsToDelete
                }
            }
        )
    }

    if (showBulkCategorizeDialog && selectedTxIds.isNotEmpty()) {
        BulkCategorizeTransactionsDialog(
            selectedCount = selectedTxIds.size,
            customCategories = uiState.customOptionsMap["TX_CATEGORY"].orEmpty(),
            onAddCustomCategory = { onAddCustomOption("TX_CATEGORY", it) },
            onDismiss = { showBulkCategorizeDialog = false },
            onConfirmCategory = { newCat ->
                onBulkCategorizeTransactions(selectedTxIds, newCat)
                showBulkCategorizeDialog = false
                selectedTxIds = emptySet()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Delete / Categorize menu appears right when the user lifts their finger off the screen
        if (isBatchSelectionMode && !isFingerPressedSelecting) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                TransactionBatchActionBar(
                    selectedCount = selectedTxIds.size,
                    totalSelectableCount = filteredTransactions.size,
                    onSelectAll = { selectedTxIds = filteredTransactions.map { it.id }.toSet() },
                    onClearSelection = { selectedTxIds = emptySet() },
                    onCategorizeSelected = { showBulkCategorizeDialog = true },
                    onDeleteSelected = {
                        pendingDeleteTxIds = selectedTxIds
                    }
                )
            }
        }

        LazyColumn(
            state = lazyListState,
            userScrollEnabled = !isFingerPressedSelecting,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    dragCoords[0] = pos.y
                    dragCoords[1] = pos.y + coords.size.height
                }
                .dismissKeyboardOnSwipeDown()
                .testTag("activity_screen"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transaction Activity",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VaultTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenAddTransaction,
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("activity_add_tx_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Single-tab "Choose" selector for Transaction Type Filter (with Reset option)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        VaultChooseSelector(
                            label = "Filter by Transaction Type (${filteredTransactions.size} shown)",
                            selectedItem = selectedTypeFilter,
                            options = TransactionType.entries,
                            onSelect = { selectedTypeFilter = it },
                            itemLabel = { it.userLabel },
                            itemIcon = { transactionTypeIcon(it) },
                            placeholder = "Choose",
                            testTag = "activity_type_choose_selector"
                        )
                    }
                    if (selectedTypeFilter != null) {
                        OutlinedButton(
                            onClick = { selectedTypeFilter = null },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Filter", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    VaultGlassCard {
                        Text(
                            text = "No matching transactions recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextSecondary
                        )
                    }
                }
            } else {
                items(
                    items = filteredTransactions,
                    key = { it.id },
                    contentType = { "tx_row" }
                ) { tx ->
                    TransactionRowCard(
                        tx = tx,
                        isHidden = isHidden,
                        onReverseClick = { pendingDeleteTxIds = setOf(tx.id) },
                        isBatchSelectionMode = isBatchSelectionMode,
                        isSelected = tx.id in selectedTxIds,
                        onBoundsMeasured = { id, range -> itemBoundsInRoot[id] = range },
                        onToggleSelect = { id ->
                            selectedTxIds = if (id in selectedTxIds) selectedTxIds - id else selectedTxIds + id
                        },
                        onStartHoldDragSelection = { id ->
                            dragAnchorTxId = id
                            dragInitialIds = selectedTxIds + id
                            selectedTxIds = dragInitialIds
                        },
                        onDragSelectRange = { startY, currentY ->
                            dragCoords[2] = startY
                            dragCoords[3] = currentY
                            updateSelectionForRange(startY, currentY)
                        },
                        onDragStateChanged = { dragging, startY, currentY ->
                            isFingerPressedSelecting = dragging
                            dragCoords[2] = startY
                            dragCoords[3] = currentY
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(68.dp)) }
        }
    }
}

/**
 * Investments Screen (Equities, ETFs, Mutual Funds, SIPs, FD, RD, Bonds, Gold, Silver).
 * Strictly distinguishes Invested Amount, Current Recorded Value (never fabricated!),
 * Realized P&L, Unrealized P&L, Interest/Dividends, CAGR, XIRR, and FD/RD Projected Maturity.
 */
@Composable
fun InvestmentsScreen(
    uiState: FinanceVaultUiState,
    onAddInvestment: (
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
    ) -> Unit,
    onUpdateRecordedValuation: (Long, Long?) -> Unit,
    onAddCustomOption: (String, String) -> Unit = { _, _ -> }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingValuationInv by remember { mutableStateOf<InvestmentEntity?>(null) }
    val isHidden = uiState.settings.isPrivacyModeEnabled

    val totalInvested = remember(uiState.investments) { uiState.investments.sumOf { it.investedAmountPaise } }
    val hasAnyRecordedValuation = remember(uiState.investments) {
        uiState.investments.any { it.isCurrentValueRecorded && it.currentRecordedValuePaise != null }
    }
    val totalCurrentRecorded = remember(uiState.investments) {
        uiState.investments.sumOf {
            if (it.isCurrentValueRecorded && it.currentRecordedValuePaise != null) it.currentRecordedValuePaise
            else it.investedAmountPaise
        }
    }
    val totalUnrealized = if (hasAnyRecordedValuation) totalCurrentRecorded - totalInvested else 0L
    val totalRealized = remember(uiState.investments) { uiState.investments.sumOf { it.realizedProfitLossPaise } }
    val totalInterestAndDiv = remember(uiState.investments) {
        uiState.investments.sumOf { it.interestEarnedPaise + it.dividendsReceivedPaise }
    }

    // Portfolio XIRR from actual investment purchase cash flows and current recorded valuation
    val portfolioXirr = remember(uiState.investments) {
        if (uiState.investments.isEmpty() || !hasAnyRecordedValuation) null
        else {
            val flows = mutableListOf<FinancialCalculators.CashFlowPoint>()
            uiState.investments.forEach { inv ->
                flows.add(FinancialCalculators.CashFlowPoint(inv.purchaseDateEpochDay, -(inv.investedAmountPaise + inv.chargesPaise)))
            }
            flows.add(FinancialCalculators.CashFlowPoint(IndianFinancialYear.todayEpochDay(), totalCurrentRecorded))
            FinancialCalculators.calculateXirrPercent(flows)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("investments_screen"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Investment Portfolio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VaultTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_investment_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Holding", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Portfolio Summary Card
        item {
            VaultGlassCard(accentColor = VaultCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Invested Capital", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                            Text(
                                text = if (uiState.investments.isEmpty()) "Not recorded"
                                else InrMoney.formatPaise(totalInvested, isHidden = isHidden),
                                style = MaterialTheme.typography.titleLarge,
                                color = VaultTextPrimary
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Recorded Value", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                            Text(
                                text = when {
                                    uiState.investments.isEmpty() -> "Not recorded"
                                    !hasAnyRecordedValuation -> "Awaiting valuation"
                                    else -> InrMoney.formatPaise(totalCurrentRecorded, isHidden = isHidden)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = VaultCyan
                            )
                        }
                    }

                    HorizontalDivider(color = VaultCardElevated)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unrealized P&L", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                            Text(
                                text = if (!hasAnyRecordedValuation) "Not recorded"
                                else InrMoney.formatPaise(totalUnrealized, isHidden = isHidden),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (totalUnrealized >= 0) VaultEmerald else VaultCoral
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Realized P&L", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                            Text(
                                text = if (uiState.investments.isEmpty()) "Not recorded"
                                else InrMoney.formatPaise(totalRealized, isHidden = isHidden),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (totalRealized >= 0) VaultEmerald else VaultCoral
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Int & Div", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                            Text(
                                text = if (uiState.investments.isEmpty()) "Not recorded"
                                else InrMoney.formatPaise(totalInterestAndDiv, isHidden = isHidden),
                                style = MaterialTheme.typography.titleSmall,
                                color = VaultGold
                            )
                        }
                    }

                    if (portfolioXirr != null) {
                        Text(
                            text = "Portfolio Annualized XIRR: $portfolioXirr% p.a.",
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultEmerald
                        )
                    }
                }
            }
        }

        if (uiState.investments.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        text = "No Investment Holdings Recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                }
            }
        } else {
            items(
                items = uiState.investments,
                key = { it.id },
                contentType = { "investment_card" }
            ) { inv ->
                InvestmentHoldingCard(
                    inv = inv,
                    isHidden = isHidden,
                    onEditValuation = { editingValuationInv = inv }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showAddDialog) {
        AddInvestmentDialog(
            accounts = uiState.accountsWithBalances,
            customCategories = uiState.customOptionsMap["INVESTMENT_CATEGORY"].orEmpty(),
            onAddCustomCategory = { onAddCustomOption("INVESTMENT_CATEGORY", it) },
            onDismiss = { showAddDialog = false },
            onSave = { name, cat, date, qty, units, price, invested, charges, curVal, isCurRec, payAcc, rate, comp, dur, notes ->
                onAddInvestment(name, cat, date, qty, units, price, invested, charges, curVal, isCurRec, payAcc, rate, comp, dur, notes)
                showAddDialog = false
            }
        )
    }

    if (editingValuationInv != null) {
        val target = editingValuationInv!!
        var valInput by remember(target) {
            mutableStateOf(
                target.currentRecordedValuePaise?.let { InrMoney.paiseToPlainRupeeInput(it) } ?: ""
            )
        }
        AlertDialog(
            onDismissRequest = { editingValuationInv = null },
            containerColor = VaultCardSurface,
            title = { Text("Update Recorded Value: ${target.name}", color = VaultTextPrimary) },
            text = {
                OutlinedTextField(
                    value = valInput,
                    onValueChange = { valInput = it },
                    label = { Text("Current Recorded Value (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = InrMoney.parseInrToPaise(valInput)
                        onUpdateRecordedValuation(target.id, parsed)
                        editingValuationInv = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                ) {
                    Text("Save Valuation")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingValuationInv = null }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InvestmentHoldingCard(
    inv: InvestmentEntity,
    isHidden: Boolean,
    onEditValuation: () -> Unit
) {
    val isDeposit = inv.category == InvestmentCategory.FIXED_DEPOSIT || inv.category == InvestmentCategory.RECURRING_DEPOSIT
    val projectedMaturity = remember(inv) {
        if (isDeposit && inv.annualInterestRatePercent != null && inv.durationMonths != null) {
            if (inv.category == InvestmentCategory.FIXED_DEPOSIT) {
                FinancialCalculators.calculateFixedDepositMaturity(
                    principalPaise = inv.investedAmountPaise,
                    annualRatePercent = inv.annualInterestRatePercent,
                    durationMonths = inv.durationMonths,
                    compoundingPerYear = inv.compoundingFrequency.periodsPerYear
                )
            } else {
                val monthlyInst = inv.investedAmountPaise / inv.durationMonths.coerceAtLeast(1)
                FinancialCalculators.calculateRecurringDepositMaturity(
                    monthlyInstallmentPaise = monthlyInst,
                    annualRatePercent = inv.annualInterestRatePercent,
                    tenureMonths = inv.durationMonths,
                    compoundingPerYear = inv.compoundingFrequency.periodsPerYear
                )
            }
        } else null
    }

    val holdingDays = (IndianFinancialYear.todayEpochDay() - inv.purchaseDateEpochDay).coerceAtLeast(1L)
    val cagr = if (inv.isCurrentValueRecorded && inv.currentRecordedValuePaise != null) {
        FinancialCalculators.calculateCagrPercent(inv.investedAmountPaise, inv.currentRecordedValuePaise, holdingDays)
    } else null

    VaultGlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VaultEmerald.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = investmentCategoryIcon(inv.category),
                            contentDescription = null,
                            tint = VaultEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(inv.name, style = MaterialTheme.typography.titleMedium, color = VaultTextPrimary)
                        Text(
                            text = "${inv.category.displayName} • Purchased ${IndianFinancialYear.formatEpochDay(inv.purchaseDateEpochDay)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextSecondary
                        )
                    }
                }
                OutlinedButton(
                    onClick = onEditValuation,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update Value", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider(color = VaultCardElevated)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Invested Amount", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                    Text(
                        text = InrMoney.formatPaise(inv.investedAmountPaise, isHidden = isHidden),
                        style = MaterialTheme.typography.titleSmall,
                        color = VaultTextPrimary
                    )
                }
                Column {
                    Text("Recorded Current Value", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                    Text(
                        text = InrMoney.formatOptionalPaise(
                            paise = inv.currentRecordedValuePaise,
                            isRecorded = inv.isCurrentValueRecorded,
                            missingLabel = "Not recorded",
                            isHidden = isHidden
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = VaultCyan
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Unrealized P&L", style = MaterialTheme.typography.labelSmall, color = VaultTextSecondary)
                    if (inv.isCurrentValueRecorded && inv.currentRecordedValuePaise != null) {
                        val diff = inv.currentRecordedValuePaise - inv.investedAmountPaise
                        val absRet = FinancialCalculators.calculateAbsoluteReturnPercent(inv.investedAmountPaise, inv.currentRecordedValuePaise)
                        Text(
                            text = "${InrMoney.formatPaise(diff, isHidden = isHidden)} ($absRet%)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (diff >= 0) VaultEmerald else VaultCoral
                        )
                    } else {
                        Text("Not recorded", style = MaterialTheme.typography.titleSmall, color = VaultTextMuted)
                    }
                }
            }

            if (cagr != null) {
                Text(
                    text = "CAGR: $cagr% p.a.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultEmerald
                )
            }

            if (projectedMaturity != null) {
                val (matPaise, projInterestPaise) = projectedMaturity
                Text(
                    text = "Projected Maturity (${inv.annualInterestRatePercent}% p.a., ${inv.compoundingFrequency.displayName}): ${InrMoney.formatPaise(matPaise, isHidden = isHidden)} (Projected Interest: ${InrMoney.formatPaise(projInterestPaise, isHidden = isHidden)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultGold
                )
            }
        }
    }
}

@Composable
private fun AddInvestmentDialog(
    accounts: List<AccountWithBalance>,
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val bankAndCashAccounts = remember(accounts) {
        accounts.map { it.account }.filter { !it.isArchived && it.accountType != AccountType.CREDIT_CARD }
    }

    var name by remember { mutableStateOf("") }
    // Always start unselected on "Choose" (null) — never pre-select a default!
    var category by remember { mutableStateOf<InvestmentCategory?>(null) }
    var customCategoryLabel by remember { mutableStateOf("") }
    var investedInput by remember { mutableStateOf("") }
    var currentValInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("1") }
    var chargesInput by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var rateInput by remember { mutableStateOf("") }
    var durationMonthsInput by remember { mutableStateOf("") }
    var compounding by remember { mutableStateOf<CompoundingFrequency?>(null) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var highlightMissing by remember { mutableStateOf(false) }
    var catExpandTrigger by remember { mutableIntStateOf(0) }
    val nameFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }

    val isDeposit = category == InvestmentCategory.FIXED_DEPOSIT || category == InvestmentCategory.RECURRING_DEPOSIT || category == InvestmentCategory.BONDS

    fun submitInvestment() {
        keyboardController?.hide()
        val chosenCategory = category
        if (chosenCategory == null) {
            highlightMissing = true
            errorMsg = "Please select an Investment Category from the opened menu."
            catExpandTrigger++
            return
        }
        if (name.isBlank()) {
            highlightMissing = true
            errorMsg = "Please enter an investment name."
            scope.launch {
                runCatching { nameFocusRequester.requestFocus() }
                keyboardController?.show()
            }
            return
        }
        val invPaise = InrMoney.parseInrToPaise(investedInput)
        if (invPaise == null || invPaise <= 0L) {
            highlightMissing = true
            errorMsg = "Please enter a valid invested amount > ₹0."
            scope.launch {
                runCatching { amountFocusRequester.requestFocus() }
                keyboardController?.show()
            }
            return
        }
        val curPaise = InrMoney.parseInrToPaise(currentValInput)
        val units = unitsInput.toDoubleOrNull()?.coerceAtLeast(0.0001) ?: 1.0
        val pricePerUnit = (invPaise / units).toLong()
        val charges = InrMoney.parseInrToPaise(chargesInput) ?: 0L
        val rate = rateInput.toDoubleOrNull()
        val dur = durationMonthsInput.toIntOrNull()
        val finalNotes = if (customCategoryLabel.isNotBlank()) customCategoryLabel else notes

        onSave(
            name,
            chosenCategory,
            IndianFinancialYear.todayEpochDay(),
            units,
            units,
            pricePerUnit,
            invPaise,
            charges,
            curPaise,
            currentValInput.isNotBlank() && curPaise != null,
            selectedAccountId,
            rate,
            compounding ?: CompoundingFrequency.QUARTERLY,
            dur,
            finalNotes
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = { Text("Add Investment Holding", color = VaultTextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .dismissKeyboardOnSwipeDown()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultChooseSelector(
                    label = "Investment Category",
                    selectedItem = category,
                    options = InvestmentCategory.entries,
                    onSelect = {
                        category = it
                        errorMsg = null
                    },
                    itemLabel = { it.displayName },
                    itemIcon = { investmentCategoryIcon(it) },
                    placeholder = "Choose",
                    testTag = "investment_category_choose_selector",
                    autoExpandTrigger = catExpandTrigger,
                    isHighlightedUnselected = highlightMissing && category == null,
                    customOptions = customCategories,
                    customSelectedLabel = customCategoryLabel.ifBlank { null },
                    onSelectCustomOption = { customCategoryLabel = it },
                    onAddCustomOption = {
                        onAddCustomCategory(it)
                        customCategoryLabel = it
                    }
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    isError = highlightMissing && name.isBlank(),
                    label = { Text("Investment Name (Required)") },
                    placeholder = { Text("e.g. Nifty 50 Index Fund, 1-Yr Fixed Deposit") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitInvestment() }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester)
                        .testTag("investment_name_input")
                )

                OutlinedTextField(
                    value = investedInput,
                    onValueChange = {
                        investedInput = InrMoney.sanitizeIndianNumericInput(it)
                        errorMsg = null
                    },
                    isError = highlightMissing && (InrMoney.parseInrToPaise(investedInput) ?: 0L) <= 0L,
                    visualTransformation = IndianCurrencyVisualTransformation,
                    label = { Text("Total Invested Amount in INR (₹)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitInvestment() }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester)
                        .testTag("investment_amount_input")
                )

                OutlinedTextField(
                    value = currentValInput,
                    onValueChange = { currentValInput = InrMoney.sanitizeIndianNumericInput(it) },
                    visualTransformation = IndianCurrencyVisualTransformation,
                    label = { Text("Current Recorded Value (₹, Optional)") },
                    placeholder = { Text("Leave blank if not yet known (never fabricated)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitInvestment() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        label = { Text("Units / Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = chargesInput,
                        onValueChange = { chargesInput = InrMoney.sanitizeIndianNumericInput(it) },
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Charges / Brokerage (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isDeposit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it },
                            label = { Text("Interest Rate (% p.a.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationMonthsInput,
                            onValueChange = { durationMonthsInput = it },
                            label = { Text("Tenure (Months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    VaultChooseSelector(
                        label = "Compounding Frequency",
                        selectedItem = compounding,
                        options = CompoundingFrequency.entries,
                        onSelect = { compounding = it },
                        itemLabel = { it.displayName },
                        itemIcon = { Icons.Default.Autorenew },
                        placeholder = "Choose"
                    )
                }

                val fundingOptions = remember(bankAndCashAccounts) {
                    listOf<Long?>(null) + bankAndCashAccounts.map { it.id }
                }
                VaultChooseSelector(
                    label = "Funding Source",
                    selectedItem = selectedAccountId,
                    options = fundingOptions,
                    onSelect = { selectedAccountId = it },
                    itemLabel = { accId ->
                        if (accId == null) "Existing Holding (Opening Capital)"
                        else "Paid from ${bankAndCashAccounts.firstOrNull { it.id == accId }?.name ?: "Account"}"
                    },
                    itemIcon = { accId ->
                        val acc = bankAndCashAccounts.firstOrNull { it.id == accId }
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
                onClick = { submitInvestment() },
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                modifier = Modifier.testTag("save_investment_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Holding", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}
