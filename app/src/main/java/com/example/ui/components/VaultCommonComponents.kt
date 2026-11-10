package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountType
import com.example.data.local.BillingFrequency
import com.example.data.local.BudgetPeriod
import com.example.data.local.CompoundingFrequency
import com.example.data.local.GiftCardEntity
import com.example.data.local.GoalType
import com.example.data.local.InvestmentCategory
import com.example.data.local.InvestmentEntity
import com.example.data.local.LoanEntity
import com.example.data.local.LoanType
import com.example.data.local.TaxType
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.export.DocumentExporter
import com.example.domain.location.LocationHelper
import com.example.domain.money.InrMoney
import com.example.domain.period.IndianFinancialYear
import com.example.ui.AccountWithBalance
import com.example.ui.theme.VaultBorderSubtle
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
import com.example.ui.theme.VaultThemeState
import kotlinx.coroutines.launch

/**
 * Pass-through gesture modifier that automatically hides the soft keyboard whenever the user swipes downward,
 * without interfering with normal vertical scrolling.
 */
fun Modifier.dismissKeyboardOnSwipeDown(): Modifier = composed {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var startY = down.position.y
            var triggered = false
            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull()
                if (change != null && !triggered) {
                    val dy = change.position.y - startY
                    if (dy > 28f) {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = false)
                        triggered = true
                    } else if (dy < -10f) {
                        startY = change.position.y
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * Automatic Indian Numbering System Comma VisualTransformation (e.g., 1,23,456.78).
 * Formats the first 3 digits from the right and every 2 digits thereafter, while maintaining
 * exact monotonic cursor offset mappings.
 */
object IndianCurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val intEnd = original.indexOf('.').let { if (it == -1) original.length else it }
        var totalIntDigits = 0
        for (i in 0 until intEnd) {
            if (original[i].isDigit()) totalIntDigits++
        }

        if (totalIntDigits <= 3) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val sb = StringBuilder()
        val origToTrans = IntArray(original.length + 1)
        val transToOrigList = mutableListOf<Int>()

        var intDigitIndex = 0
        for (origIdx in original.indices) {
            val ch = original[origIdx]
            if (origIdx < intEnd && ch.isDigit()) {
                val rem = totalIntDigits - intDigitIndex
                if (intDigitIndex > 0 && rem >= 3 && (rem - 3) % 2 == 0) {
                    sb.append(',')
                    transToOrigList.add(origIdx)
                }
                intDigitIndex++
            }
            origToTrans[origIdx] = sb.length
            sb.append(ch)
            transToOrigList.add(origIdx)
        }
        origToTrans[original.length] = sb.length
        transToOrigList.add(original.length)

        val transformedString = sb.toString()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, original.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrigList[offset.coerceIn(0, transformedString.length)]
            }
        }
        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }
}

/**
 * Custom High-Contrast Toggle Button (Switch) designed for both Dark Mode and Light Mode.
 * Clearly distinguishes ON and OFF states with crisp borders, distinct tracks, ON/OFF labels,
 * and an elevated icon thumb so it never looks washed-out or muddy when toggled OFF.
 */
@Composable
fun VaultToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isLight = VaultThemeState.isLightTheme
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B).copy(alpha = 0.5f)
            checked -> VaultEmerald
            isLight -> Color(0xFFCBD5E1)
            else -> Color(0xFF1E293B)
        },
        animationSpec = tween(durationMillis = 180),
        label = "toggle_track_color"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155)
            checked -> if (isLight) Color(0xFF047857) else Color(0xFF34D399)
            isLight -> Color(0xFF64748B)
            else -> Color(0xFF64748B)
        },
        animationSpec = tween(durationMillis = 180),
        label = "toggle_border_color"
    )
    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> if (isLight) Color(0xFFF1F5F9) else Color(0xFF64748B)
            checked -> Color.White
            isLight -> Color.White
            else -> Color(0xFFE2E8F0)
        },
        animationSpec = tween(durationMillis = 180),
        label = "toggle_thumb_color"
    )
    val thumbStartPadding by animateDpAsState(
        targetValue = if (checked) 32.dp else 4.dp,
        animationSpec = tween(durationMillis = 180),
        label = "toggle_thumb_offset"
    )

    Box(
        modifier = modifier
            .size(width = 60.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        // Explicit ON / OFF state label inside the track
        Text(
            text = if (checked) "ON" else "OFF",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when {
                !enabled -> VaultTextMuted
                checked -> if (isLight) Color.White else Color(0xFF032218)
                isLight -> Color(0xFF334155)
                else -> Color(0xFFCBD5E1)
            },
            modifier = Modifier
                .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 8.dp)
        )

        // Elevated circular thumb with Check / Close icon
        Box(
            modifier = Modifier
                .padding(start = thumbStartPadding)
                .size(24.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (checked) "Enabled" else "Disabled",
                tint = when {
                    !enabled -> VaultTextMuted
                    checked -> VaultEmerald
                    isLight -> Color(0xFF475569)
                    else -> Color(0xFF1E293B)
                },
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * High-contrast pill toggle button for multi-option tasks (e.g., Light/Dark/System Theme or Report Tabs)
 * so unselected/toggled-off options look sharp and well-defined in both Dark and Light themes.
 */
@Composable
fun VaultOptionTogglePill(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val isLight = VaultThemeState.isLightTheme
    val bgColor = when {
        selected -> if (isLight) VaultEmerald else VaultEmerald.copy(alpha = 0.22f)
        isLight -> Color(0xFFE2E8F0)
        else -> Color(0xFF1E293B)
    }
    val borderColor = when {
        selected -> VaultEmerald
        isLight -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }
    val contentColor = when {
        selected -> if (isLight) Color.White else VaultEmerald
        isLight -> Color(0xFF1E293B)
        else -> Color(0xFFE2E8F0)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * Icon mapping helpers for icon-first UX across all screens and selectors.
 */
fun transactionTypeIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    TransactionType.CASH_DEPOSIT -> Icons.Default.Savings
    TransactionType.CASH_WITHDRAWAL -> Icons.Default.Payments
    TransactionType.CREDIT_CARD_PURCHASE -> Icons.Default.CreditCard
    TransactionType.CREDIT_CARD_REPAYMENT -> Icons.Default.CreditCard
    TransactionType.INVESTMENT_PURCHASE -> Icons.Default.AutoGraph
    TransactionType.INVESTMENT_SALE -> Icons.Default.WorkspacePremium
    TransactionType.FD_MATURITY -> Icons.Default.Savings
    TransactionType.RD_CONTRIBUTION -> Icons.Default.EventRepeat
    TransactionType.RD_MATURITY -> Icons.Default.Savings
    TransactionType.LOAN_RECEIVED -> Icons.Default.AccountBalance
    TransactionType.LOAN_REPAYMENT -> Icons.Default.Payments
    TransactionType.INTEREST_RECEIVED -> Icons.Default.Percent
    TransactionType.DIVIDEND_RECEIVED -> Icons.Default.AttachMoney
    TransactionType.REFUND_RECEIVED -> Icons.Default.Replay
    TransactionType.GIFT_CARD_PURCHASE -> Icons.Default.CardGiftcard
    TransactionType.GIFT_CARD_USAGE -> Icons.Default.CardGiftcard
    TransactionType.TAX_PAYMENT -> Icons.Default.Gavel
    TransactionType.BANK_CHARGE -> Icons.AutoMirrored.Filled.ReceiptLong
    TransactionType.OPENING_BALANCE -> Icons.Default.AccountBalanceWallet
    TransactionType.REVERSAL -> Icons.Default.Replay
    TransactionType.CORRECTION -> Icons.Default.Build
}

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Default.Savings
    AccountType.SAVINGS -> Icons.Default.AccountBalance
    AccountType.CURRENT -> Icons.Default.AccountBalance
    AccountType.BANK -> Icons.Default.AccountBalance
    AccountType.DIGITAL_WALLET -> Icons.Default.AccountBalanceWallet
    AccountType.UPI -> Icons.Default.QrCode2
    AccountType.CREDIT_CARD -> Icons.Default.CreditCard
    AccountType.OTHER -> Icons.Default.AccountBalanceWallet
}

fun investmentCategoryIcon(cat: InvestmentCategory): ImageVector = when (cat) {
    InvestmentCategory.EQUITY_SHARES -> Icons.Default.AutoGraph
    InvestmentCategory.ETF -> Icons.Default.PieChart
    InvestmentCategory.MUTUAL_FUND -> Icons.Default.AutoGraph
    InvestmentCategory.SIP -> Icons.Default.EventRepeat
    InvestmentCategory.FIXED_DEPOSIT -> Icons.Default.Savings
    InvestmentCategory.RECURRING_DEPOSIT -> Icons.Default.Savings
    InvestmentCategory.BONDS -> Icons.Default.AccountBalance
    InvestmentCategory.GOVT_SECURITIES -> Icons.Default.AccountBalance
    InvestmentCategory.GOLD -> Icons.Default.WorkspacePremium
    InvestmentCategory.SILVER -> Icons.Default.WorkspacePremium
    InvestmentCategory.OTHER -> Icons.Default.Category
}

fun loanTypeIcon(type: LoanType): ImageVector = when (type) {
    LoanType.PERSONAL -> Icons.Default.Payments
    LoanType.EDUCATION -> Icons.Default.School
    LoanType.HOME -> Icons.Default.Home
    LoanType.VEHICLE -> Icons.Default.DirectionsCar
    LoanType.OTHER -> Icons.Default.CreditCard
}

fun goalTypeIcon(type: GoalType): ImageVector = when (type) {
    GoalType.SAVINGS -> Icons.Default.Savings
    GoalType.EMERGENCY_FUND -> Icons.Default.Security
    GoalType.PURCHASE -> Icons.Default.ShoppingBag
}

fun taxTypeIcon(type: TaxType): ImageVector = when (type) {
    TaxType.INCOME_TAX -> Icons.Default.Gavel
    TaxType.TDS -> Icons.Default.Percent
    TaxType.ADVANCE_TAX -> Icons.AutoMirrored.Filled.ReceiptLong
    TaxType.SELF_ASSESSMENT_TAX -> Icons.Default.AccountBalance
    TaxType.GST -> Icons.Default.Payments
    TaxType.OTHER -> Icons.Default.Gavel
}

fun categoryIcon(cat: String): ImageVector {
    val lower = cat.lowercase()
    return when {
        "salary" in lower || "income" in lower -> Icons.AutoMirrored.Filled.TrendingUp
        "household" in lower || "grocer" in lower -> Icons.Default.ShoppingBag
        "utilit" in lower || "bill" in lower -> Icons.Default.Build
        "transport" in lower || "fuel" in lower || "car" in lower -> Icons.Default.DirectionsCar
        "educat" in lower || "school" in lower -> Icons.Default.School
        "health" in lower || "medical" in lower -> Icons.Default.LocalHospital
        "tax" in lower || "gst" in lower || "tds" in lower -> Icons.Default.Gavel
        else -> Icons.Default.Category
    }
}

/**
 * Single-tab "Choose" selector component used across all windows.
 * Never pre-selects a default when `selectedItem == null`; always shows "Choose" with an icon,
 * and opens a single-click icon menu on tap.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> VaultChooseSelector(
    label: String,
    selectedItem: T?,
    options: List<T>,
    onSelect: (T) -> Unit,
    itemLabel: (T) -> String,
    itemIcon: (T) -> ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
    testTag: String? = null,
    autoExpandTrigger: Int = 0,
    isHighlightedUnselected: Boolean = false,
    customOptions: List<String> = emptyList(),
    customSelectedLabel: String? = null,
    onSelectCustomOption: ((String) -> Unit)? = null,
    onAddCustomOption: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf("") }
    var pendingOtherOption by remember { mutableStateOf<T?>(null) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val isSelected = selectedItem != null || !customSelectedLabel.isNullOrBlank()
    val needsAttention = !isSelected && isHighlightedUnselected
    val accentColor = when {
        isSelected -> VaultEmerald
        needsAttention -> VaultCoral
        else -> VaultCyan
    }
    val borderColor = when {
        isSelected -> VaultEmerald.copy(alpha = 0.65f)
        needsAttention -> VaultCoral
        else -> VaultCyan.copy(alpha = 0.55f)
    }

    fun isOtherLabel(text: String): Boolean {
        val clean = text.trim()
        return clean.equals("Other", ignoreCase = true) ||
            clean.equals("Other Income", ignoreCase = true) ||
            clean.equals("Other Expenses", ignoreCase = true) ||
            clean.equals("Other Expense", ignoreCase = true)
    }

    LaunchedEffect(autoExpandTrigger) {
        if (autoExpandTrigger > 0 && !isSelected) {
            runCatching { bringIntoViewRequester.bringIntoView() }
            expanded = true
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = VaultCardSurface,
            title = {
                Text(
                    text = "Add Custom $label",
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = customInputText,
                    onValueChange = { customInputText = it },
                    label = { Text("Enter custom name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_option_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = customInputText.trim()
                        if (clean.isNotEmpty()) {
                            pendingOtherOption?.let { onSelect(it) }
                            @Suppress("UNCHECKED_CAST")
                            (onSelect as? (String) -> Unit)?.invoke(clean)
                            onAddCustomOption?.invoke(clean)
                            onSelectCustomOption?.invoke(clean)
                            customInputText = ""
                            showCustomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    modifier = Modifier.testTag("confirm_custom_option_button")
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                if (anchorWidthPx != it.size.width) {
                    anchorWidthPx = it.size.width
                }
            }
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VaultCardElevated)
                .border(if (needsAttention) 1.8.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .let { if (testTag != null) it.testTag(testTag) else it },
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedItem != null) itemIcon(selectedItem) else Icons.Default.TouchApp,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (needsAttention) VaultCoral else VaultTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            !customSelectedLabel.isNullOrBlank() -> customSelectedLabel
                            selectedItem != null -> itemLabel(selectedItem)
                            needsAttention -> "$placeholder (Required)"
                            else -> placeholder
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSelected) VaultTextPrimary else accentColor,
                        fontWeight = if (needsAttention) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Choose $label",
                tint = accentColor
            )
        }

        val existingLabels = remember(options) { options.map { itemLabel(it).lowercase() }.toSet() }
        val extraSavedOptions = remember(customOptions, existingLabels) {
            customOptions.filter { it.lowercase() !in existingLabels }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .let {
                    if (anchorWidthPx > 0) {
                        it.width(with(density) { anchorWidthPx.toDp() })
                    } else {
                        it.fillMaxWidth()
                    }
                }
                .background(VaultCardSurface)
                .heightIn(max = 360.dp)
        ) {
            options.forEach { option ->
                val optLabel = itemLabel(option)
                val active = (option == selectedItem) && customSelectedLabel.isNullOrBlank()
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = itemIcon(option),
                            contentDescription = null,
                            tint = if (active) VaultEmerald else VaultCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = optLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (active) VaultEmerald else VaultTextPrimary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        if (isOtherLabel(optLabel) && (onAddCustomOption != null || onSelectCustomOption != null)) {
                            pendingOtherOption = option
                            customInputText = ""
                            showCustomDialog = true
                        } else {
                            onSelectCustomOption?.invoke("")
                            onSelect(option)
                        }
                    }
                )
            }

            extraSavedOptions.forEach { customOpt ->
                val active = customSelectedLabel.equals(customOpt, ignoreCase = true) ||
                    (selectedItem as? String)?.equals(customOpt, ignoreCase = true) == true
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIcon(customOpt),
                            contentDescription = null,
                            tint = if (active) VaultEmerald else VaultCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = customOpt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (active) VaultEmerald else VaultTextPrimary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        val otherOpt = options.firstOrNull { isOtherLabel(itemLabel(it)) }
                        if (otherOpt != null) {
                            onSelect(otherOpt)
                        }
                        @Suppress("UNCHECKED_CAST")
                        (onSelect as? (String) -> Unit)?.invoke(customOpt)
                        onSelectCustomOption?.invoke(customOpt)
                    }
                )
            }
        }
    }
}

/**
 * Startup loading screen with a load bar so the Home dashboard never flashes before the PIN or Biometric unlock screen.
 */
@Composable
fun VaultStartupLoadingScreen() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("startup_loading_screen"),
        color = VaultObsidianBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(VaultEmerald.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Finance Vault Security Check",
                    tint = VaultEmerald,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Finance Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = VaultTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Made by Lenin and Shaswat",
                style = MaterialTheme.typography.labelMedium,
                color = VaultEmerald
            )
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("startup_loading_bar"),
                color = VaultEmerald,
                trackColor = VaultCardElevated
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Verifying security lock...",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextSecondary
            )
        }
    }
}

private val VaultGlassCardShape = RoundedCornerShape(16.dp)

@Composable
fun VaultGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = VaultBorderSubtle,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val topColor = VaultCardElevated
    val bottomColor = VaultCardSurface
    val cardBrush = remember(topColor, bottomColor) {
        Brush.verticalGradient(
            colors = listOf(topColor.copy(alpha = 0.94f), bottomColor)
        )
    }
    val baseMod = modifier
        .fillMaxWidth()
        .clip(VaultGlassCardShape)
        .background(cardBrush)
        .border(1.dp, accentColor.copy(alpha = 0.45f), VaultGlassCardShape)
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .padding(13.dp)

    Box(modifier = baseMod) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = VaultTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = VaultEmerald
                )
            }
        }
    }
}

@Composable
fun AssetDistributionChart(
    cashPaise: Long,
    bankPaise: Long,
    investmentsPaise: Long,
    otherAssetsPaise: Long,
    isHidden: Boolean
) {
    val total = (cashPaise + bankPaise + investmentsPaise + otherAssetsPaise).coerceAtLeast(0L)
    val slices = listOf(
        Triple("Bank Balances", bankPaise.coerceAtLeast(0L), VaultCyan),
        Triple("Investments", investmentsPaise.coerceAtLeast(0L), VaultEmerald),
        Triple("Cash & Wallets", cashPaise.coerceAtLeast(0L), VaultGold),
        Triple("Other / Gift Cards", otherAssetsPaise.coerceAtLeast(0L), VaultPurple)
    )

    VaultGlassCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "Asset Allocation (INR ₹)",
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (total == 0L) {
                Text(
                    text = "Awaiting information — Add accounts or investments to view asset distribution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextSecondary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Canvas(modifier = Modifier.size(92.dp)) {
                        val strokeWidth = 18.dp.toPx()
                        var startAngle = -90f
                        for ((_, amt, color) in slices) {
                            if (amt > 0L) {
                                val sweep = (amt.toFloat() / total.toFloat()) * 360f
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                                )
                                startAngle += sweep
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((label, amt, color) in slices) {
                            val pct = InrMoney.percentageRatio(amt, total)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VaultTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (isHidden) "••%" else "$pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VaultTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseChartCard(
    incomePaise: Long,
    expensePaise: Long,
    periodLabel: String,
    isHidden: Boolean
) {
    val maxVal = maxOf(incomePaise, expensePaise, 1L)
    val surplus = incomePaise - expensePaise

    VaultGlassCard {
        Column {
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
                        Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = VaultEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Income vs Expenses ($periodLabel)",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                }
                Text(
                    text = if (surplus >= 0) "Surplus: ${InrMoney.formatPaise(surplus, isHidden = isHidden)}"
                    else "Deficit: ${InrMoney.formatPaise(surplus, isHidden = isHidden)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (surplus >= 0) VaultEmerald else VaultCoral
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (incomePaise == 0L && expensePaise == 0L) {
                Text(
                    text = "Not recorded for this period yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextSecondary
                )
            } else {
                // Money In bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = VaultEmerald, modifier = Modifier.size(14.dp))
                            Text("Money In (Income)", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                        }
                        Text(InrMoney.formatPaise(incomePaise, isHidden = isHidden), style = MaterialTheme.typography.labelMedium, color = VaultEmerald)
                    }
                    val incFraction = (incomePaise.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(VaultObsidianBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(incFraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(VaultEmerald)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Money Out bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = VaultCoral, modifier = Modifier.size(14.dp))
                            Text("Money Out (Expenses)", style = MaterialTheme.typography.bodySmall, color = VaultTextSecondary)
                        }
                        Text(InrMoney.formatPaise(expensePaise, isHidden = isHidden), style = MaterialTheme.typography.labelMedium, color = VaultCoral)
                    }
                    val expFraction = (expensePaise.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(VaultObsidianBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(expFraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(VaultCoral)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    isBiometricEnabled: Boolean = false,
    onBiometricUnlockSuccess: () -> Unit = {},
    onUnlockAttempt: (String) -> Boolean
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            com.example.domain.security.BiometricAuthHelper.promptBiometricUnlock(
                context = context,
                onSuccess = onBiometricUnlockSuccess,
                onErrorMessage = { msg -> errorMsg = msg }
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnSwipeDown(),
        color = VaultObsidianBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(VaultEmerald.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = VaultEmerald,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Finance Vault Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = VaultTextPrimary
            )
            Text(
                text = if (isBiometricEnabled) {
                    "Authenticate with Fingerprint / Face ID or enter your backup PIN"
                } else {
                    "Enter your security PIN to access your financial records"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = VaultTextSecondary
            )
            Spacer(modifier = Modifier.height(22.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    pinInput = it.filter { ch -> ch.isDigit() }.take(8)
                    errorMsg = null
                },
                label = { Text("Security PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        val ok = onUnlockAttempt(pinInput)
                        if (!ok) {
                            errorMsg = "Incorrect PIN. Please try again."
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_unlock_input")
            )
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMsg!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    val ok = onUnlockAttempt(pinInput)
                    if (!ok) {
                        errorMsg = "Incorrect PIN. Please try again."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pin_unlock_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with PIN", fontWeight = FontWeight.Bold)
            }

            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        errorMsg = null
                        com.example.domain.security.BiometricAuthHelper.promptBiometricUnlock(
                            context = context,
                            onSuccess = onBiometricUnlockSuccess,
                            onErrorMessage = { msg -> errorMsg = msg }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("biometric_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = VaultCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Fingerprint / Face ID", color = VaultCyan, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Made by Lenin and Shaswat",
                style = MaterialTheme.typography.labelSmall,
                color = VaultTextMuted
            )
        }
    }
}

@Composable
fun ExportPreviewDialog(
    result: DocumentExporter.ExportedDocumentResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var saveFeedbackMessage by remember(result) {
        mutableStateOf(
            if (result.downloadedLocationLabel.isNotBlank()) {
                "✓ Automatically downloaded to: ${result.downloadedLocationLabel}"
            } else {
                "✓ Ready to download: ${result.file.name}"
            }
        )
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(result.mimeType)
    ) { targetUri ->
        if (targetUri != null) {
            val ok = DocumentExporter.copyFileToUri(context, result.file, targetUri)
            if (ok) {
                saveFeedbackMessage = "✓ Saved copy to your chosen device folder (${result.file.name})"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Text(
                text = "Download & Preview: ${result.title}",
                style = MaterialTheme.typography.titleLarge,
                color = VaultTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download confirmation banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultEmerald.copy(alpha = 0.16f))
                        .border(1.dp, VaultEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = VaultEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = saveFeedbackMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = VaultEmerald
                    )
                }

                // Action row for Save As / Open / Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val (_, dlLabel) = DocumentExporter.saveFileToDownloads(context, result.file, result.mimeType)
                            saveFeedbackMessage = "✓ Downloaded to $dlLabel"
                            runCatching {
                                createDocumentLauncher.launch(result.file.name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_download_export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download / Save As", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { DocumentExporter.openExportedFile(context, result) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_exported_file_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open File", tint = VaultCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open File", color = VaultCyan, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultObsidianBg)
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = result.previewText,
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    DocumentExporter.shareExportedFile(context, result)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                modifier = Modifier.testTag("confirm_share_export_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = VaultObsidianBg, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share File", color = VaultObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = VaultTextSecondary)
            }
        }
    )
}

/**
 * Unified Transaction Recording Dialog with Icon-First UX, single-tab "Choose" selectors (no pre-selected defaults),
 * keyboard Done auto-submission, and swipe-down keyboard dismissal.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordTransactionDialog(
    accounts: List<AccountWithBalance>,
    investments: List<InvestmentEntity>,
    loans: List<LoanEntity>,
    giftCards: List<GiftCardEntity>,
    transactions: List<TransactionEntity>,
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSubmit: (
        transactionType: TransactionType,
        dateEpochDay: Long,
        amountPaise: Long,
        principalComponentPaise: Long,
        interestComponentPaise: Long,
        chargesPaise: Long,
        costBasisDisposedPaise: Long,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        category: String,
        description: String,
        referenceNumber: String,
        notes: String,
        locationName: String,
        latitude: Double?,
        longitude: Double?,
        linkedInvestmentId: Long?,
        linkedLoanId: Long?,
        linkedGiftCardId: Long?,
        linkedOriginalTransactionId: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectableTypes = remember {
        listOf(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            TransactionType.TRANSFER,
            TransactionType.CREDIT_CARD_PURCHASE,
            TransactionType.CREDIT_CARD_REPAYMENT,
            TransactionType.INVESTMENT_PURCHASE,
            TransactionType.INVESTMENT_SALE,
            TransactionType.LOAN_RECEIVED,
            TransactionType.LOAN_REPAYMENT,
            TransactionType.INTEREST_RECEIVED,
            TransactionType.DIVIDEND_RECEIVED,
            TransactionType.REFUND_RECEIVED,
            TransactionType.CASH_DEPOSIT,
            TransactionType.CASH_WITHDRAWAL,
            TransactionType.BANK_CHARGE,
            TransactionType.TAX_PAYMENT,
            TransactionType.GIFT_CARD_USAGE,
            TransactionType.FD_MATURITY
        )
    }

    val nonCardAccounts = remember(accounts) {
        accounts.map { it.account }.filter { !it.isArchived && it.accountType != AccountType.CREDIT_CARD }
    }
    val creditCardAccounts = remember(accounts) {
        accounts.map { it.account }.filter { !it.isArchived && it.accountType == AccountType.CREDIT_CARD }
    }
    val allActiveAccounts = remember(accounts) {
        accounts.map { it.account }.filter { !it.isArchived }
    }

    // Always start on "Choose" (null) — never pre-select a default!
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }
    var amountInput by remember { mutableStateOf("") }
    var principalInput by remember { mutableStateOf("") }
    var interestInput by remember { mutableStateOf("") }
    var chargesInput by remember { mutableStateOf("") }
    var costDisposedInput by remember { mutableStateOf("") }

    var sourceAccId by remember { mutableStateOf<Long?>(null) }
    var destAccId by remember { mutableStateOf<Long?>(null) }
    var selectedInvestmentId by remember { mutableStateOf<Long?>(null) }
    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var selectedGiftCardId by remember { mutableStateOf<Long?>(null) }
    var selectedOrigTxId by remember(transactions) {
        mutableStateOf(transactions.firstOrNull { it.transactionType == TransactionType.EXPENSE }?.id)
    }

    val defaultCategories = remember(selectedType, customCategories) {
        val base = when (selectedType) {
            TransactionType.INCOME -> listOf("Salary Income", "Business Income", "Freelance Income", "Other")
            TransactionType.EXPENSE, TransactionType.CREDIT_CARD_PURCHASE, TransactionType.GIFT_CARD_USAGE ->
                listOf("Household Expenses", "Utilities & Bills", "Transportation Expenses", "Education Expenses", "Healthcare & Medical", "Other")
            TransactionType.INTEREST_RECEIVED -> listOf("Interest Income", "Other")
            TransactionType.DIVIDEND_RECEIVED -> listOf("Dividend Income", "Other")
            TransactionType.TAX_PAYMENT -> listOf("Income Tax", "Advance Tax", "TDS", "GST", "Other")
            TransactionType.BANK_CHARGE -> listOf("Bank Charges & Fees", "Other")
            null -> emptyList()
            else -> listOf(selectedType!!.userLabel, "Other")
        }
        if (base.isEmpty()) emptyList() else (base.filterNot { it.equals("Other", ignoreCase = true) } + customCategories + "Other").distinct()
    }

    var selectedCategory by remember(selectedType) {
        mutableStateOf<String?>(if (defaultCategories.size == 1) defaultCategories.first() else null)
    }
    var descriptionInput by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var locationNameInput by remember { mutableStateOf("") }
    var capturedLat by remember { mutableStateOf<Double?>(null) }
    var capturedLng by remember { mutableStateOf<Double?>(null) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Auto-scroll & auto-open triggers for unselected areas when pressing Done / Tick
    var highlightMissing by remember { mutableStateOf(false) }
    var typeExpandTrigger by remember { mutableIntStateOf(0) }
    var sourceExpandTrigger by remember { mutableIntStateOf(0) }
    var destExpandTrigger by remember { mutableIntStateOf(0) }
    var categoryExpandTrigger by remember { mutableIntStateOf(0) }
    var investmentExpandTrigger by remember { mutableIntStateOf(0) }
    var loanExpandTrigger by remember { mutableIntStateOf(0) }
    var giftCardExpandTrigger by remember { mutableIntStateOf(0) }
    val amountFocusRequester = remember { FocusRequester() }
    val amountBringIntoViewRequester = remember { BringIntoViewRequester() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                isFetchingGps = true
                val loc = LocationHelper.fetchCurrentLocation(context)
                if (loc != null) {
                    capturedLat = loc.latitude
                    capturedLng = loc.longitude
                    if (locationNameInput.isBlank()) {
                        locationNameInput = loc.placeName
                    }
                }
                isFetchingGps = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            isFetchingGps = true
            val loc = LocationHelper.fetchCurrentLocation(context)
            if (loc != null) {
                capturedLat = loc.latitude
                capturedLng = loc.longitude
                if (locationNameInput.isBlank()) {
                    locationNameInput = loc.placeName
                }
            }
            isFetchingGps = false
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val needsSourceAccount = selectedType in setOf(
        TransactionType.EXPENSE,
        TransactionType.TRANSFER,
        TransactionType.CASH_DEPOSIT,
        TransactionType.CASH_WITHDRAWAL,
        TransactionType.CREDIT_CARD_PURCHASE,
        TransactionType.CREDIT_CARD_REPAYMENT,
        TransactionType.INVESTMENT_PURCHASE,
        TransactionType.LOAN_REPAYMENT,
        TransactionType.BANK_CHARGE,
        TransactionType.TAX_PAYMENT
    )

    val needsDestAccount = selectedType in setOf(
        TransactionType.INCOME,
        TransactionType.TRANSFER,
        TransactionType.CASH_DEPOSIT,
        TransactionType.CASH_WITHDRAWAL,
        TransactionType.CREDIT_CARD_REPAYMENT,
        TransactionType.INVESTMENT_SALE,
        TransactionType.FD_MATURITY,
        TransactionType.LOAN_RECEIVED,
        TransactionType.INTEREST_RECEIVED,
        TransactionType.DIVIDEND_RECEIVED,
        TransactionType.REFUND_RECEIVED
    )

    /**
     * Moves to and automatically opens/focuses the first unselected or missing required area
     * when the user presses the keyboard Tick/Done button or the dialog Done/Post button.
     */
    fun attemptSubmit(fromKeyboardDone: Boolean = false) {
        keyboardController?.hide()
        val chosenType = selectedType
        if (chosenType == null) {
            highlightMissing = true
            validationError = "Please select a Transaction Type from the opened menu."
            typeExpandTrigger++
            return
        }

        val resolvedSourceId = sourceAccId ?: if (needsSourceAccount && allActiveAccounts.size == 1) allActiveAccounts.first().id else null
        val resolvedDestId = destAccId ?: if (needsDestAccount && allActiveAccounts.size == 1) allActiveAccounts.first().id else null

        if (needsSourceAccount && resolvedSourceId == null) {
            highlightMissing = true
            validationError = "Please select the From Account from the opened menu."
            sourceExpandTrigger++
            return
        }
        if (needsDestAccount && resolvedDestId == null) {
            highlightMissing = true
            validationError = "Please select the Into Account from the opened menu."
            destExpandTrigger++
            return
        }
        if (defaultCategories.size > 1 && selectedCategory == null) {
            highlightMissing = true
            validationError = "Please select a Category from the opened menu."
            categoryExpandTrigger++
            return
        }
        if (chosenType in setOf(TransactionType.INVESTMENT_PURCHASE, TransactionType.INVESTMENT_SALE, TransactionType.FD_MATURITY) &&
            selectedInvestmentId == null && investments.size > 1
        ) {
            highlightMissing = true
            validationError = "Please select the Linked Investment Holding from the opened menu."
            investmentExpandTrigger++
            return
        }
        if (chosenType in setOf(TransactionType.LOAN_RECEIVED, TransactionType.LOAN_REPAYMENT) &&
            selectedLoanId == null && loans.size > 1
        ) {
            highlightMissing = true
            validationError = "Please select the Linked Loan Account from the opened menu."
            loanExpandTrigger++
            return
        }
        if (chosenType == TransactionType.GIFT_CARD_USAGE &&
            selectedGiftCardId == null && giftCards.size > 1
        ) {
            highlightMissing = true
            validationError = "Please select the Gift Card Used from the opened menu."
            giftCardExpandTrigger++
            return
        }

        val amountPaise = InrMoney.parseInrToPaise(amountInput)
        if (amountPaise == null || amountPaise <= 0L) {
            highlightMissing = true
            validationError = "Please enter a valid INR amount greater than ₹0."
            scope.launch {
                runCatching { amountBringIntoViewRequester.bringIntoView() }
                runCatching { amountFocusRequester.requestFocus() }
                keyboardController?.show()
            }
            return
        }

        val resolvedCategory = selectedCategory ?: defaultCategories.firstOrNull() ?: chosenType.userLabel
        val principalPaise = InrMoney.parseInrToPaise(principalInput) ?: 0L
        val interestPaise = InrMoney.parseInrToPaise(interestInput) ?: 0L
        val chargesPaise = InrMoney.parseInrToPaise(chargesInput) ?: 0L
        val costDisposedPaise = InrMoney.parseInrToPaise(costDisposedInput) ?: 0L

        if (chosenType == TransactionType.LOAN_REPAYMENT &&
            (principalPaise > 0L || interestPaise > 0L) &&
            (principalPaise + interestPaise != amountPaise)
        ) {
            validationError = "Loan principal + interest must equal total repayment amount."
            return
        }

        onSubmit(
            chosenType,
            IndianFinancialYear.todayEpochDay(),
            amountPaise,
            principalPaise,
            interestPaise,
            chargesPaise,
            costDisposedPaise,
            resolvedSourceId,
            resolvedDestId,
            resolvedCategory,
            descriptionInput.ifBlank { resolvedCategory },
            referenceInput,
            notesInput,
            locationNameInput,
            capturedLat,
            capturedLng,
            selectedInvestmentId ?: investments.firstOrNull()?.id,
            selectedLoanId ?: loans.firstOrNull()?.id,
            selectedGiftCardId ?: giftCards.firstOrNull()?.id,
            selectedOrigTxId
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
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
                            .background(VaultEmerald.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = VaultEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Record Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .dismissKeyboardOnSwipeDown()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allActiveAccounts.isEmpty()) {
                    VaultGlassCard(accentColor = VaultGold) {
                        Text(
                            text = "Add an account in the Accounts tab first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultGold
                        )
                    }
                }

                // Single-tab "Choose" selector for Transaction Type (No default pre-selected!)
                VaultChooseSelector(
                    label = "Transaction Type",
                    selectedItem = selectedType,
                    options = selectableTypes,
                    onSelect = { type ->
                        selectedType = type
                        validationError = null
                        sourceAccId = null
                        destAccId = null
                    },
                    itemLabel = { it.userLabel },
                    itemIcon = { transactionTypeIcon(it) },
                    placeholder = "Choose",
                    testTag = "tx_type_choose_selector",
                    autoExpandTrigger = typeExpandTrigger,
                    isHighlightedUnselected = highlightMissing && selectedType == null
                )

                // Source Account "Choose" Selector
                if (needsSourceAccount) {
                    val candidateSources = if (selectedType == TransactionType.CREDIT_CARD_PURCHASE) {
                        creditCardAccounts
                    } else {
                        nonCardAccounts.ifEmpty { allActiveAccounts }
                    }
                    val selectedSourceAcc = candidateSources.firstOrNull { it.id == sourceAccId }
                    VaultChooseSelector(
                        label = if (selectedType == TransactionType.CREDIT_CARD_PURCHASE) "Credit Card Used" else "From Account (Money Out)",
                        selectedItem = selectedSourceAcc,
                        options = candidateSources,
                        onSelect = { acc ->
                            sourceAccId = acc.id
                            validationError = null
                        },
                        itemLabel = { "${it.name} (${it.accountType.displayName})" },
                        itemIcon = { accountTypeIcon(it.accountType) },
                        placeholder = "Choose",
                        testTag = "tx_source_account_choose_selector",
                        autoExpandTrigger = sourceExpandTrigger,
                        isHighlightedUnselected = highlightMissing && selectedSourceAcc == null
                    )
                }

                // Destination Account "Choose" Selector
                if (needsDestAccount) {
                    val candidateDests = if (selectedType == TransactionType.CREDIT_CARD_REPAYMENT) {
                        creditCardAccounts
                    } else {
                        nonCardAccounts.ifEmpty { allActiveAccounts }
                    }
                    val selectedDestAcc = candidateDests.firstOrNull { it.id == destAccId }
                    VaultChooseSelector(
                        label = if (selectedType == TransactionType.CREDIT_CARD_REPAYMENT) "To Credit Card Account" else "Into Account (Money In)",
                        selectedItem = selectedDestAcc,
                        options = candidateDests,
                        onSelect = { acc ->
                            destAccId = acc.id
                            validationError = null
                        },
                        itemLabel = { "${it.name} (${it.accountType.displayName})" },
                        itemIcon = { accountTypeIcon(it.accountType) },
                        placeholder = "Choose",
                        testTag = "tx_dest_account_choose_selector",
                        autoExpandTrigger = destExpandTrigger,
                        isHighlightedUnselected = highlightMissing && selectedDestAcc == null
                    )
                }

                // Category "Choose" Selector with custom "Other" pop-up
                if (defaultCategories.isNotEmpty()) {
                    VaultChooseSelector(
                        label = "Category",
                        selectedItem = selectedCategory,
                        options = defaultCategories,
                        onSelect = { cat ->
                            selectedCategory = cat
                            validationError = null
                        },
                        itemLabel = { it },
                        itemIcon = { categoryIcon(it) },
                        placeholder = "Choose",
                        testTag = "tx_category_choose_selector",
                        autoExpandTrigger = categoryExpandTrigger,
                        isHighlightedUnselected = highlightMissing && selectedCategory == null,
                        customOptions = customCategories,
                        onAddCustomOption = { customCat ->
                            onAddCustomCategory(customCat)
                            selectedCategory = customCat
                            validationError = null
                        }
                    )
                }

                // Amount Input with Automatic Indian Comma Formatting (1,23,456) & ImeAction.Done Auto-Submit
                val isAmountMissing = highlightMissing && (InrMoney.parseInrToPaise(amountInput) ?: 0L) <= 0L
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = InrMoney.sanitizeIndianNumericInput(it)
                        validationError = null
                    },
                    isError = isAmountMissing,
                    visualTransformation = IndianCurrencyVisualTransformation,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = "INR Amount",
                            tint = if (isAmountMissing) VaultCoral else VaultEmerald
                        )
                    },
                    label = { Text("Amount in INR (₹) — Press Done to Post") },
                    placeholder = { Text("e.g. 1,25,000") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { attemptSubmit(fromKeyboardDone = true) }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(amountBringIntoViewRequester)
                        .focusRequester(amountFocusRequester)
                        .testTag("tx_amount_input")
                )

                // Investment selector & sale fields
                if (selectedType in setOf(TransactionType.INVESTMENT_PURCHASE, TransactionType.INVESTMENT_SALE, TransactionType.FD_MATURITY)) {
                    val selectedInv = investments.firstOrNull { it.id == selectedInvestmentId }
                    VaultChooseSelector(
                        label = "Linked Investment Holding",
                        selectedItem = selectedInv,
                        options = investments,
                        onSelect = {
                            selectedInvestmentId = it.id
                            validationError = null
                        },
                        itemLabel = { "${it.name} (${it.category.displayName})" },
                        itemIcon = { investmentCategoryIcon(it.category) },
                        placeholder = "Choose",
                        autoExpandTrigger = investmentExpandTrigger,
                        isHighlightedUnselected = highlightMissing && selectedInv == null
                    )
                    if (selectedType == TransactionType.INVESTMENT_SALE) {
                        OutlinedTextField(
                            value = costDisposedInput,
                            onValueChange = { costDisposedInput = InrMoney.sanitizeIndianNumericInput(it) },
                            visualTransformation = IndianCurrencyVisualTransformation,
                            label = { Text("Original Cost of Units Sold (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { attemptSubmit(fromKeyboardDone = true) }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = chargesInput,
                        onValueChange = { chargesInput = InrMoney.sanitizeIndianNumericInput(it) },
                        visualTransformation = IndianCurrencyVisualTransformation,
                        label = { Text("Brokerage / Charges (₹, Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { attemptSubmit(fromKeyboardDone = true) }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Loan selector & Principal + Interest breakdown
                if (selectedType in setOf(TransactionType.LOAN_RECEIVED, TransactionType.LOAN_REPAYMENT)) {
                    val selectedLoan = loans.firstOrNull { it.id == selectedLoanId }
                    VaultChooseSelector(
                        label = "Linked Loan Account",
                        selectedItem = selectedLoan,
                        options = loans,
                        onSelect = {
                            selectedLoanId = it.id
                            validationError = null
                        },
                        itemLabel = { "${it.name} (${it.loanType.displayName})" },
                        itemIcon = { loanTypeIcon(it.loanType) },
                        placeholder = "Choose",
                        autoExpandTrigger = loanExpandTrigger,
                        isHighlightedUnselected = highlightMissing && selectedLoan == null
                    )
                    if (selectedType == TransactionType.LOAN_REPAYMENT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = principalInput,
                                onValueChange = { principalInput = InrMoney.sanitizeIndianNumericInput(it) },
                                visualTransformation = IndianCurrencyVisualTransformation,
                                label = { Text("Principal (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = interestInput,
                                onValueChange = { interestInput = InrMoney.sanitizeIndianNumericInput(it) },
                                visualTransformation = IndianCurrencyVisualTransformation,
                                label = { Text("Interest (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { attemptSubmit(fromKeyboardDone = true) }),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (selectedType == TransactionType.GIFT_CARD_USAGE) {
                    val selectedGc = giftCards.firstOrNull { it.id == selectedGiftCardId }
                    VaultChooseSelector(
                        label = "Gift Card Used",
                        selectedItem = selectedGc,
                        options = giftCards,
                        onSelect = {
                            selectedGiftCardId = it.id
                            validationError = null
                        },
                        itemLabel = { "${it.brand} (${InrMoney.formatPaise(it.remainingValuePaise)})" },
                        itemIcon = { Icons.Default.CardGiftcard },
                        placeholder = "Choose",
                        autoExpandTrigger = giftCardExpandTrigger,
                        isHighlightedUnselected = highlightMissing && selectedGc == null
                    )
                }

                // Single Location / Place bar with inline GPS icon (device automatically checks GPS)
                OutlinedTextField(
                    value = locationNameInput,
                    onValueChange = { locationNameInput = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = VaultEmerald
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    scope.launch {
                                        isFetchingGps = true
                                        val loc = LocationHelper.fetchCurrentLocation(context)
                                        if (loc != null) {
                                            capturedLat = loc.latitude
                                            capturedLng = loc.longitude
                                            locationNameInput = loc.placeName
                                        }
                                        isFetchingGps = false
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("detect_location_service_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS",
                                tint = if (capturedLat != null && capturedLng != null) VaultEmerald else VaultCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    label = { Text("Location / Place") },
                    placeholder = { Text("e.g. Connaught Place, Indiranagar") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { attemptSubmit(fromKeyboardDone = true) }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_location_input")
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "Description",
                            tint = VaultCyan
                        )
                    },
                    label = { Text("Particulars / Description (Optional)") },
                    placeholder = { Text("e.g. Monthly groceries, Salary credit") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { attemptSubmit(fromKeyboardDone = true) }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_description_input")
                )

                if (validationError != null) {
                    Text(text = validationError!!, color = VaultCoral, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { attemptSubmit(fromKeyboardDone = false) },
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                modifier = Modifier.testTag("submit_transaction_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Done / Post", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}
