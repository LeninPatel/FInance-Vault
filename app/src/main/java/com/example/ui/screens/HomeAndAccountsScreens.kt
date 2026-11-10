package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountType
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.domain.security.PinSecurityUtil
import com.example.ui.AccountWithBalance
import com.example.ui.FinanceVaultUiState
import com.example.ui.MainTab
import com.example.ui.MoreDestination
import com.example.ui.ReportTab
import com.example.ui.components.AssetDistributionChart
import com.example.ui.components.IncomeVsExpenseChartCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.VaultGlassCard
import com.example.ui.theme.VaultCardElevated
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultObsidianBg
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import com.example.ui.components.IndianCurrencyVisualTransformation
import com.example.ui.components.VaultChooseSelector
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.categoryIcon
import com.example.ui.components.dismissKeyboardOnSwipeDown
import com.example.ui.components.transactionTypeIcon
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.min

val BULK_TRANSACTION_CATEGORIES = listOf(
    "Household & Groceries",
    "Food & Dining",
    "Housing & Rent",
    "Utilities & Recharge",
    "Transportation & Fuel",
    "Shopping & Lifestyle",
    "Education",
    "Healthcare & Medical",
    "Entertainment & Subscriptions",
    "Salary Income",
    "Business & Freelance",
    "Rental Income",
    "Other"
)

@Composable
fun DashboardScreen(
    uiState: FinanceVaultUiState,
    searchQuery: String = "",
    onTogglePrivacy: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onNavigateMore: (MoreDestination) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onReverseTransaction: (Long) -> Unit = {},
    onBulkReverseTransactions: (Set<Long>) -> Unit = {},
    onBulkCategorizeTransactions: (Set<Long>, String) -> Unit = { _, _ -> },
    onAddCustomOption: (String, String) -> Unit = { _, _ -> },
    topVoiceAndRecallBanner: (@Composable () -> Unit)? = null
) {
    val isHidden = uiState.settings.isPrivacyModeEnabled
    val bs = uiState.balanceSheet
    val hasRecords = uiState.hasAnyUserRecords
    val filteredRecentTransactions = remember(uiState.transactions, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.transactions.take(12)
        } else {
            uiState.transactions.filter { tx ->
                tx.matchesSearchQuery(searchQuery)
            }
        }
    }

    val lazyListState = rememberLazyListState()
    // Non-snapshot coordinate refs so onGloballyPositioned never causes Compose snapshot writes during scroll
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
            val anchorIdx = filteredRecentTransactions.indexOfFirst { it.id == anchorId }
            val hitIndices = filteredRecentTransactions.mapIndexedNotNull { idx, item ->
                if (item.id in swipedIds) idx else null
            }
            if (anchorIdx >= 0 && hitIndices.isNotEmpty()) {
                val minIdx = min(anchorIdx, hitIndices.minOrNull() ?: anchorIdx)
                val maxIdx = max(anchorIdx, hitIndices.maxOrNull() ?: anchorIdx)
                filteredRecentTransactions.subList(minIdx, maxIdx + 1).map { it.id }.toSet()
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
        // Delete / Batch menu appears immediately when the user lifts their finger off the screen
        if (isBatchSelectionMode && !isFingerPressedSelecting) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                TransactionBatchActionBar(
                    selectedCount = selectedTxIds.size,
                    totalSelectableCount = filteredRecentTransactions.size,
                    onSelectAll = { selectedTxIds = filteredRecentTransactions.map { it.id }.toSet() },
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
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (topVoiceAndRecallBanner != null) {
                item(key = "top_voice_recall_banner") {
                    topVoiceAndRecallBanner()
                }
            }

            // Immediate Search Results at the very top when searching from Home
            if (searchQuery.isNotBlank()) {
                item {
                    SectionHeader(
                        title = "Search Results (\"$searchQuery\") • ${filteredRecentTransactions.size} found",
                        actionLabel = "All Activity",
                        onAction = { onNavigateTab(MainTab.ACTIVITY) }
                    )
                }
                if (filteredRecentTransactions.isEmpty()) {
                    item {
                        VaultGlassCard(accentColor = VaultCoral) {
                            Text(
                                text = "No transactions found matching \"$searchQuery\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultTextSecondary
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredRecentTransactions.take(15),
                        key = { "top_search_${it.id}" },
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
                item {
                    HorizontalDivider(color = VaultCardElevated, modifier = Modifier.padding(vertical = 4.dp))
                }
            }

        // Demo Mode Banner when active
        if (uiState.settings.isDemoModeEnabled) {
            item {
                VaultGlassCard(accentColor = VaultGold) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEMONSTRATION MODE",
                            style = MaterialTheme.typography.labelLarge,
                            color = VaultGold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onToggleDemoMode(false) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("exit_demo_mode_button")
                        ) {
                            Text("Exit Demo", color = VaultGold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Hero Net Worth Card (Apple Wallet-inspired, Pixel 4a 393dp proportioned)
        item {
            VaultGlassCard(accentColor = VaultEmerald) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = VaultEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "PERSONAL NET WORTH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (!hasRecords) "Not recorded"
                                else InrMoney.formatWithLakhCroreHint(bs.totalNetWorthPaise, isHidden = isHidden),
                                style = MaterialTheme.typography.headlineLarge,
                                color = VaultTextPrimary,
                                modifier = Modifier.testTag("hero_net_worth_value")
                            )
                        }
                        IconButton(
                            onClick = onTogglePrivacy,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("privacy_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isHidden) "Show amounts" else "Hide amounts",
                                tint = VaultTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = VaultCardElevated)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricColumn(
                            modifier = Modifier.weight(1f),
                            label = "Total Assets",
                            value = if (!hasRecords) "Not recorded"
                            else InrMoney.formatPaise(bs.totalAssetsPaise, isHidden = isHidden),
                            valueColor = VaultEmerald
                        )
                        MetricColumn(
                            modifier = Modifier.weight(1f),
                            label = "Liabilities",
                            value = if (!hasRecords) "Not recorded"
                            else InrMoney.formatPaise(bs.totalLiabilitiesPaise, isHidden = isHidden),
                            valueColor = if (bs.totalLiabilitiesPaise > 0) VaultCoral else VaultTextPrimary
                        )
                        MetricColumn(
                            modifier = Modifier.weight(1f),
                            label = "Cash & Bank",
                            value = if (uiState.accountsWithBalances.isEmpty()) "Not provided"
                            else InrMoney.formatPaise(
                                bs.currentCashAndEquivalentsPaise + bs.currentBankBalancesPaise,
                                isHidden = isHidden
                            ),
                            valueColor = VaultCyan
                        )
                    }
                }
            }
        }

        // First-launch quick actions when empty
        if (!hasRecords) {
            item {
                VaultGlassCard(accentColor = VaultCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateTab(MainTab.ACCOUNTS) },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("empty_add_account_cta")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Account", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { onToggleDemoMode(true) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("enable_demo_mode_cta")
                        ) {
                            Text("Try Demo", color = VaultCyan, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Monthly Performance & Key Balances Grid
        item {
            val mPnl = uiState.monthlyProfitAndLoss
            val monthlySurplus = mPnl.surplusOrDeficitPaise
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultGlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = VaultEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Monthly Income", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!hasRecords) "Not recorded" else InrMoney.formatPaise(mPnl.totalIncomePaise, isHidden = isHidden),
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Savings Rate: ${if (!hasRecords) "—" else "${uiState.monthlySavingsRatePercent}%"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextMuted
                        )
                    }
                }
                VaultGlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = VaultCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Monthly Expenses", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!hasRecords) "Not recorded" else InrMoney.formatPaise(mPnl.totalExpensesPaise, isHidden = isHidden),
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultCoral
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Monthly Net: ${if (!hasRecords) "—" else InrMoney.formatPaise(monthlySurplus, isHidden = isHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (monthlySurplus >= 0) VaultEmerald else VaultCoral
                        )
                    }
                }
            }
        }

        // Investments, Loans, Credit Card Dues & Gift Cards Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultGlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(MainTab.INVESTMENTS) }
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(15.dp))
                            Text("Investments", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.investments.isEmpty()) "Not recorded"
                            else InrMoney.formatPaise(bs.nonCurrentInvestmentsPaise, isHidden = isHidden),
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultCyan
                        )
                        Text(
                            text = "${uiState.investments.size} holding(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextMuted
                        )
                    }
                }
                VaultGlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateMore(MoreDestination.LOANS) }
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = VaultGold, modifier = Modifier.size(15.dp))
                            Text("Loans & Cards", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!hasRecords) "Not recorded"
                            else InrMoney.formatPaise(
                                bs.nonCurrentLoansAndBorrowingsPaise + bs.currentCreditCardOutstandingPaise,
                                isHidden = isHidden
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (bs.totalLiabilitiesPaise > 0) VaultGold else VaultTextPrimary
                        )
                        Text(
                            text = "Card Dues: ${if (!hasRecords) "—" else InrMoney.formatPaise(bs.currentCreditCardOutstandingPaise, isHidden = isHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextMuted
                        )
                    }
                }
            }
        }

        // Quick Access to Statements (Current Balance & Financial Report Status)
        item {
            val currentBalanceText = if (!hasRecords) {
                "₹0.00"
            } else {
                InrMoney.formatPaise(bs.totalNetWorthPaise, isHidden = isHidden)
            }
            val currentReportSummary = if (!hasRecords) {
                "Balance Sheet • P&L • Trial Balance"
            } else {
                val pnlSign = if (uiState.fyProfitAndLoss.surplusOrDeficitPaise >= 0L) "+" else ""
                "Net P&L (${uiState.settings.activeFinancialYear}): $pnlSign${InrMoney.formatPaise(uiState.fyProfitAndLoss.surplusOrDeficitPaise, isHidden = isHidden)}"
            }
            VaultGlassCard(
                accentColor = VaultCyan,
                onClick = { onNavigateMore(MoreDestination.REPORTS) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Statements",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = VaultTextPrimary
                            )
                            Text(
                                text = currentReportSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Balance: $currentBalanceText",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = VaultEmerald
                        )
                        Text(
                            text = if (bs.isReconciled && uiState.trialBalance.isAgreed) "Balanced Report" else "Current Report",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultCyan
                        )
                    }
                }
            }
        }

        // Interactive Charts
        item {
            IncomeVsExpenseChartCard(
                incomePaise = uiState.fyProfitAndLoss.totalIncomePaise,
                expensePaise = uiState.fyProfitAndLoss.totalExpensesPaise,
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

        // Recent Financial Activity Timeline (Filtered by Top-Right Search Bar)
        item {
            SectionHeader(
                title = if (searchQuery.isNotBlank()) "Filtered Activity (${filteredRecentTransactions.size})" else "Recent Activity",
                actionLabel = "All Activity",
                onAction = { onNavigateTab(MainTab.ACTIVITY) }
            )
        }

        if (uiState.transactions.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        text = "No transactions recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextSecondary
                    )
                }
            }
        } else if (filteredRecentTransactions.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        text = "No transactions matching \"$searchQuery\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextSecondary
                    )
                }
            }
        } else {
            items(
                items = filteredRecentTransactions,
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

        item {
            Spacer(modifier = Modifier.height(68.dp))
        }
    }
    }
}

@Composable
private fun MetricColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextSecondary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 1
        )
    }
}

fun com.example.data.local.TransactionEntity.matchesSearchQuery(rawQuery: String): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true
    if (description.contains(q, ignoreCase = true) ||
        category.contains(q, ignoreCase = true) ||
        locationName.contains(q, ignoreCase = true) ||
        transactionType.userLabel.contains(q, ignoreCase = true) ||
        transactionType.name.contains(q, ignoreCase = true) ||
        voucherNumber.contains(q, ignoreCase = true) ||
        referenceNumber.contains(q, ignoreCase = true) ||
        notes.contains(q, ignoreCase = true)
    ) {
        return true
    }
    val amountRupeesPlain = (amountPaise / 100L).toString()
    if (amountRupeesPlain.contains(q, ignoreCase = true)) return true
    return InrMoney.formatPaise(amountPaise).contains(q, ignoreCase = true)
}

private val INFLOW_TX_TYPES = setOf(
    com.example.data.local.TransactionType.INCOME,
    com.example.data.local.TransactionType.INTEREST_RECEIVED,
    com.example.data.local.TransactionType.DIVIDEND_RECEIVED,
    com.example.data.local.TransactionType.REFUND_RECEIVED,
    com.example.data.local.TransactionType.LOAN_RECEIVED,
    com.example.data.local.TransactionType.INVESTMENT_SALE,
    com.example.data.local.TransactionType.FD_MATURITY
)

private val NEUTRAL_TRANSFER_TX_TYPES = setOf(
    com.example.data.local.TransactionType.TRANSFER,
    com.example.data.local.TransactionType.CASH_DEPOSIT,
    com.example.data.local.TransactionType.CASH_WITHDRAWAL,
    com.example.data.local.TransactionType.OPENING_BALANCE,
    com.example.data.local.TransactionType.CREDIT_CARD_REPAYMENT,
    com.example.data.local.TransactionType.INVESTMENT_PURCHASE
)

@Composable
fun TransactionRowCard(
    tx: com.example.data.local.TransactionEntity,
    isHidden: Boolean,
    onReverseClick: (() -> Unit)? = null,
    isBatchSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onBoundsMeasured: ((Long, ClosedFloatingPointRange<Float>) -> Unit)? = null,
    onToggleSelect: ((Long) -> Unit)? = null,
    onStartHoldDragSelection: ((Long) -> Unit)? = null,
    onDragSelectRange: ((Float, Float) -> Unit)? = null,
    onDragStateChanged: ((Boolean, Float, Float) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val cardRootTopYRef = remember { FloatArray(1) }

    val isInflow = tx.transactionType in INFLOW_TX_TYPES
    val isNeutralTransfer = tx.transactionType in NEUTRAL_TRANSFER_TX_TYPES

    val amtColor = when {
        tx.isReversed -> VaultTextMuted
        isNeutralTransfer -> VaultCyan
        isInflow -> VaultEmerald
        else -> VaultCoral
    }
    val prefix = when {
        isNeutralTransfer -> ""
        isInflow -> "+"
        else -> "-"
    }
    val formattedSubline = remember(tx.dateEpochDay, tx.category, tx.voucherNumber) {
        "${IndianFinancialYear.formatEpochDay(tx.dateEpochDay)} • ${tx.category} • ${tx.voucherNumber}"
    }
    val formattedAmount = remember(tx.amountPaise, isHidden, prefix) {
        "$prefix${InrMoney.formatPaise(tx.amountPaise, isHidden = isHidden)}"
    }

    val gestureModifier = if (onStartHoldDragSelection != null && onToggleSelect != null) {
        Modifier
            .onGloballyPositioned { coords ->
                val rootPos = coords.positionInRoot()
                cardRootTopYRef[0] = rootPos.y
                onBoundsMeasured?.invoke(tx.id, rootPos.y..(rootPos.y + coords.size.height))
            }
            .pointerInput(tx.id, isBatchSelectionMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startRootY = cardRootTopYRef[0] + down.position.y
                    var lastRootY = startRootY

                    if (isBatchSelectionMode) {
                        var didDragAcross = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!didDragAcross) {
                                        onToggleSelect(tx.id)
                                    }
                                    break
                                }
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    if (!didDragAcross) {
                                        didDragAcross = true
                                        onStartHoldDragSelection(tx.id)
                                    }
                                    change.consume()
                                    val currentRootY = cardRootTopYRef[0] + change.position.y
                                    lastRootY = currentRootY
                                    onDragStateChanged?.invoke(true, startRootY, currentRootY)
                                    onDragSelectRange?.invoke(startRootY, currentRootY)
                                }
                            }
                        } finally {
                            if (didDragAcross) {
                                onDragStateChanged?.invoke(false, startRootY, lastRootY)
                            }
                        }
                    } else {
                        // Standard quick long-press (0.5 seconds / 500ms) to activate Delete / Batch Selection Mode
                        // and allow continuous upward/downward auto-scroll swipe selection.
                        val holdSlopPx = 28.dp.toPx()
                        var movedPastSlop = false
                        val releasedEarly = withTimeoutOrNull(500L) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                                if (!change.pressed) return@withTimeoutOrNull true
                                if ((change.position - down.position).getDistance() > holdSlopPx) {
                                    movedPastSlop = true
                                    return@withTimeoutOrNull true
                                }
                            }
                        }

                        if (releasedEarly == null && !movedPastSlop) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartHoldDragSelection(tx.id)
                            onDragStateChanged?.invoke(true, startRootY, startRootY)
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    val currentRootY = cardRootTopYRef[0] + change.position.y
                                    lastRootY = currentRootY
                                    onDragStateChanged?.invoke(true, startRootY, currentRootY)
                                    onDragSelectRange?.invoke(startRootY, currentRootY)
                                }
                            } finally {
                                onDragStateChanged?.invoke(false, startRootY, lastRootY)
                            }
                        }
                    }
                }
            }
    } else {
        Modifier
    }

    VaultGlassCard(
        modifier = gestureModifier.testTag("tx_row_${tx.id}"),
        accentColor = if (isSelected) VaultEmerald else VaultCardElevated
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
                if (isBatchSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) VaultEmerald else VaultTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(amtColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = transactionTypeIcon(tx.transactionType),
                        contentDescription = tx.transactionType.userLabel,
                        tint = amtColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tx.description,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (tx.isReversed) VaultTextMuted else VaultTextPrimary,
                            textDecoration = if (tx.isReversed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (tx.isReversed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[REVERSED]",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedSubline,
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                    if (tx.locationName.isNotBlank() || (tx.latitude != null && tx.longitude != null)) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Spending Location",
                                tint = VaultCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val locDisplay = remember(tx.locationName, tx.latitude, tx.longitude) {
                                buildString {
                                    if (tx.locationName.isNotBlank()) append(tx.locationName)
                                    if (tx.latitude != null && tx.longitude != null) {
                                        if (isNotEmpty()) append(" • ")
                                        append(String.format(java.util.Locale.US, "%.4f, %.4f", tx.latitude, tx.longitude))
                                    }
                                }
                            }
                            Text(
                                text = locDisplay,
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultCyan
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    color = amtColor,
                    textDecoration = if (tx.isReversed) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!tx.isReversed && !isBatchSelectionMode && onReverseClick != null) {
                    IconButton(
                        onClick = onReverseClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_tx_${tx.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Transaction",
                            tint = VaultCoral.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionBatchActionBar(
    selectedCount: Int,
    totalSelectableCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCategorizeSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    VaultGlassCard(
        accentColor = VaultEmerald,
        modifier = Modifier.testTag("transaction_batch_action_bar")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = VaultEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$selectedCount Selected",
                        style = MaterialTheme.typography.titleSmall,
                        color = VaultEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedCount < totalSelectableCount) {
                        TextButton(
                            onClick = onSelectAll,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("batch_select_all_button")
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select All", style = MaterialTheme.typography.labelSmall, color = VaultCyan)
                        }
                    }
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("batch_clear_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Selection",
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCategorizeSelected,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_categorize_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Categorize", color = VaultCyan, style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCoral),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_delete_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Delete ($selectedCount)", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteTransactionsDialog(
    deleteCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Text(
                text = if (deleteCount == 1) "Delete Transaction?" else "Delete $deleteCount Transactions?",
                color = VaultCoral,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = if (deleteCount == 1) {
                    "Are you sure you want to delete this transaction? It will be marked as reversed in the ledger and your account balance will be updated."
                } else {
                    "Are you sure you want to delete these $deleteCount selected transactions? They will be marked as reversed in the ledger and your account balances will be updated."
                },
                color = VaultTextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = VaultCoral),
                modifier = Modifier.testTag("confirm_delete_transactions_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (deleteCount == 1) "Delete" else "Delete ($deleteCount)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_transactions_button")
            ) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}

@Composable
fun BulkCategorizeTransactionsDialog(
    selectedCount: Int,
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirmCategory: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var catExpandTrigger by remember { mutableIntStateOf(0) }
    var highlightMissing by remember { mutableStateOf(false) }

    val allCategories = remember(customCategories) {
        (BULK_TRANSACTION_CATEGORIES.filterNot { it.equals("Other", ignoreCase = true) } + customCategories + "Other").distinct()
    }

    val submitCategory: () -> Unit = {
        if (selectedCategory == null) {
            highlightMissing = true
            catExpandTrigger++
        } else {
            onConfirmCategory(selectedCategory!!)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Text("Categorize $selectedCount Transaction(s)", color = VaultTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VaultChooseSelector(
                    label = "New Category",
                    selectedItem = selectedCategory,
                    options = allCategories,
                    onSelect = { selectedCategory = it },
                    itemLabel = { it },
                    itemIcon = { categoryIcon(it) },
                    placeholder = "Choose",
                    testTag = "bulk_category_choose_selector",
                    autoExpandTrigger = catExpandTrigger,
                    isHighlightedUnselected = highlightMissing && selectedCategory == null,
                    customOptions = customCategories,
                    onAddCustomOption = { newCat ->
                        onAddCustomCategory(newCat)
                        selectedCategory = newCat
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = submitCategory,
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                modifier = Modifier.testTag("confirm_bulk_categorize_button")
            ) {
                Text("Apply Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Accounts Screen (Cash, Savings, Current, Bank, Digital Wallet, UPI, Credit Cards).
 */
@Composable
fun AccountsScreen(
    uiState: FinanceVaultUiState,
    onCreateAccount: (
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
    ) -> Unit,
    onArchiveAccount: (Long, Boolean) -> Unit,
    onOpenAccountLedger: (Long) -> Unit,
    onAddCustomOption: (String, String) -> Unit = { _, _ -> }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf<AccountType?>(null) }
    val isHidden = uiState.settings.isPrivacyModeEnabled

    val filteredAccounts = remember(uiState.accountsWithBalances, filterType) {
        if (filterType == null) uiState.accountsWithBalances
        else uiState.accountsWithBalances.filter { it.account.accountType == filterType }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnSwipeDown()
            .testTag("accounts_screen"),
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
                        text = "Financial Accounts",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VaultTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Account", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Single-tab "Choose" Account Type Filter with Icons
        item {
            val filterOptions = remember { listOf<AccountType?>(null) + AccountType.entries }
            VaultChooseSelector(
                label = "Filter by Account Category",
                selectedItem = filterType,
                options = filterOptions,
                onSelect = { filterType = it },
                itemLabel = { it?.displayName ?: "All Accounts (${uiState.accountsWithBalances.size})" },
                itemIcon = { if (it != null) accountTypeIcon(it) else Icons.Default.AccountBalanceWallet },
                placeholder = "Choose Account Filter (All: ${uiState.accountsWithBalances.size})",
                testTag = "accounts_filter_choose_selector"
            )
        }

        if (filteredAccounts.isEmpty()) {
            item {
                VaultGlassCard {
                    Text(
                        text = "No Accounts Recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                }
            }
        } else {
            items(
                items = filteredAccounts,
                key = { it.account.id },
                contentType = { "account_card" }
            ) { item ->
                AppleWalletAccountCard(
                    item = item,
                    isHidden = isHidden,
                    onViewLedger = {
                        item.ledgerAccount?.id?.let { onOpenAccountLedger(it) }
                    },
                    onArchiveToggle = {
                        onArchiveAccount(item.account.id, !item.account.isArchived)
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(68.dp)) }
    }

    if (showAddDialog) {
        AddAccountDialog(
            customAccountTypes = uiState.customOptionsMap["ACCOUNT_TYPE"].orEmpty(),
            onAddCustomAccountType = { onAddCustomOption("ACCOUNT_TYPE", it) },
            onDismiss = { showAddDialog = false },
            onSave = { name, type, bank, branch, accNo, ifsc, holder, upi, openPaise, isOpenRec, limit, notes ->
                onCreateAccount(name, type, bank, branch, accNo, ifsc, holder, upi, openPaise, isOpenRec, limit, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AppleWalletAccountCard(
    item: AccountWithBalance,
    isHidden: Boolean,
    onViewLedger: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    val acc = item.account
    val isCard = acc.accountType == AccountType.CREDIT_CARD
    val accent = when (acc.accountType) {
        AccountType.CASH -> VaultGold
        AccountType.CREDIT_CARD -> VaultCoral
        AccountType.UPI, AccountType.DIGITAL_WALLET -> VaultPurple
        else -> VaultCyan
    }

    VaultGlassCard(accentColor = accent, onClick = onViewLedger) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = accountTypeIcon(acc.accountType),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = buildString {
                                append(acc.accountType.displayName)
                                if (acc.bankName.isNotBlank()) append(" • ${acc.bankName}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isCard) "Outstanding Due" else "Closing Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                    val displayBal = if (!acc.isOpeningBalanceRecorded && item.totalDebitsPaise == 0L && item.totalCreditsPaise == 0L) {
                        "Awaiting info"
                    } else {
                        InrMoney.formatPaise(item.closingBalancePaise, isHidden = isHidden)
                    }
                    Text(
                        text = displayBal,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isCard && item.closingBalancePaise > 0) VaultCoral else VaultEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = VaultCardElevated)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acc: ${PinSecurityUtil.maskAccountNumber(acc.accountNumber, isHidden)} | IFSC: ${acc.ifscCode.ifBlank { "Not provided" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextMuted
                    )
                    Text(
                        text = "In: ${InrMoney.formatPaise(item.totalDebitsPaise, isHidden = isHidden)} • Out: ${InrMoney.formatPaise(item.totalCreditsPaise, isHidden = isHidden)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                }
                Row {
                    TextButton(
                        onClick = onViewLedger,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Ledger", style = MaterialTheme.typography.labelSmall, color = VaultCyan)
                    }
                    TextButton(
                        onClick = onArchiveToggle,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (acc.isArchived) "Restore" else "Archive",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    customAccountTypes: List<String> = emptyList(),
    onAddCustomAccountType: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    // Always start unselected on "Choose" (null) — never pre-select a default!
    var selectedType by remember { mutableStateOf<AccountType?>(null) }
    var customTypeLabel by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var openingBalanceInput by remember { mutableStateOf("") }
    var creditLimitInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var highlightMissing by remember { mutableStateOf(false) }
    var typeExpandTrigger by remember { mutableIntStateOf(0) }
    val nameFocusRequester = remember { FocusRequester() }

    fun submitAccount() {
        keyboardController?.hide()
        val chosenType = selectedType
        if (chosenType == null) {
            highlightMissing = true
            errorMsg = "Please select an Account Category from the opened menu."
            typeExpandTrigger++
            return
        }
        if (name.isBlank()) {
            highlightMissing = true
            errorMsg = "Please enter an account name."
            scope.launch {
                runCatching { nameFocusRequester.requestFocus() }
                keyboardController?.show()
            }
            return
        }
        val parsedOpening = InrMoney.parseInrToPaise(openingBalanceInput)
        val isOpeningRecorded = openingBalanceInput.isNotBlank() && parsedOpening != null
        val creditLimit = InrMoney.parseInrToPaise(creditLimitInput)
        val finalNotes = if (customTypeLabel.isNotBlank()) customTypeLabel else notes

        onSave(
            name,
            chosenType,
            bankName,
            branchName,
            accountNumber,
            ifscCode,
            accountHolder,
            upiId,
            parsedOpening ?: 0L,
            isOpeningRecorded,
            creditLimit,
            finalNotes
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = VaultEmerald)
                Text("Add Financial Account", style = MaterialTheme.typography.titleLarge, color = VaultTextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .dismissKeyboardOnSwipeDown()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultChooseSelector(
                    label = "Account Category",
                    selectedItem = selectedType,
                    options = AccountType.entries,
                    onSelect = {
                        selectedType = it
                        errorMsg = null
                    },
                    itemLabel = { it.displayName },
                    itemIcon = { accountTypeIcon(it) },
                    placeholder = "Choose",
                    testTag = "account_type_choose_selector",
                    autoExpandTrigger = typeExpandTrigger,
                    isHighlightedUnselected = highlightMissing && selectedType == null,
                    customOptions = customAccountTypes,
                    customSelectedLabel = customTypeLabel.ifBlank { null },
                    onSelectCustomOption = { customTypeLabel = it },
                    onAddCustomOption = {
                        onAddCustomAccountType(it)
                        customTypeLabel = it
                    }
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    isError = highlightMissing && name.isBlank(),
                    label = { Text("Account Label (Required)") },
                    placeholder = { Text("e.g. Primary Savings Account, Cash Wallet") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitAccount() }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester)
                        .testTag("account_name_input")
                )

                OutlinedTextField(
                    value = openingBalanceInput,
                    onValueChange = { openingBalanceInput = InrMoney.sanitizeIndianNumericInput(it) },
                    visualTransformation = IndianCurrencyVisualTransformation,
                    label = {
                        Text(
                            if (selectedType == AccountType.CREDIT_CARD) "Opening Card Outstanding (₹, Optional)"
                            else "Opening Balance in INR (₹, Optional)"
                        )
                    },
                    placeholder = { Text("Leave blank if not recorded yet") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitAccount() }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_opening_balance_input")
                )

                if (selectedType != null && selectedType != AccountType.CASH) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank / Institution Name (Optional)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitAccount() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType in setOf(AccountType.SAVINGS, AccountType.CURRENT, AccountType.BANK, AccountType.UPI)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("Account No. (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it },
                            label = { Text("IFSC (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = { Text("Account Holder Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == AccountType.UPI || selectedType == AccountType.DIGITAL_WALLET) {
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI Handle / Wallet ID (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == AccountType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = creditLimitInput,
                        onValueChange = { creditLimitInput = InrMoney.sanitizeIndianNumericInput(it) },
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Credit Card Total Limit (₹, Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitAccount() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submitAccount() },
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                modifier = Modifier.testTag("save_account_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}
