package com.example

import android.Manifest
import android.app.Activity
import android.app.VoiceInteractor
import android.app.assist.AssistContent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import com.example.domain.money.InrMoney
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.ui.screens.ConfirmDeleteTransactionsDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.FinanceVaultDatabase
import com.example.data.local.TransactionType
import com.example.data.repository.FinanceVaultRepository
import com.example.domain.ai.VoiceTransactionController
import com.example.domain.ai.configureAppCheck
import com.example.domain.period.IndianFinancialYear
import com.example.domain.reminder.TransactionRecallReminderManager
import com.example.ui.FinanceVaultViewModel
import com.example.ui.MainTab
import com.example.ui.MoreDestination
import com.example.ui.ReportTab
import com.example.ui.components.ExportPreviewDialog
import com.example.ui.components.GeminiVoiceAssistantDialog
import com.example.ui.components.HandsFreeVoiceAndRecallSection
import com.example.ui.components.PinLockScreen
import com.example.ui.components.RecordTransactionDialog
import com.example.ui.components.VaultStartupLoadingScreen
import com.example.ui.components.VoiceDisambiguationOverlayCard
import com.example.ui.components.dismissKeyboardOnSwipeDown
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsAndGoalsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancialReportsScreen
import com.example.ui.screens.GiftCardsScreen
import com.example.ui.screens.InvestmentsScreen
import com.example.ui.screens.LoansAndCreditCardsScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.SettingsAndAboutScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.TaxesScreen
import com.example.ui.screens.TransactionRowCard
import com.example.ui.screens.matchesSearchQuery
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultCardElevated
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultObsidianBg
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val incomingIntentState = MutableStateFlow<Intent?>(null)
    private var activeViewModel: FinanceVaultViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        TransactionRecallReminderManager.initialize(applicationContext)
        incomingIntentState.value = intent

        val database = FinanceVaultDatabase.getInstance(applicationContext)
        val repository = FinanceVaultRepository(database)

        setContent {
            val vm: FinanceVaultViewModel = viewModel(
                factory = FinanceVaultViewModel.Factory(repository)
            )
            activeViewModel = vm
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val pendingIntent by incomingIntentState.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = uiState.settings.themeMode) {
                FinanceVaultApp(
                    viewModel = vm,
                    pendingExternalIntent = pendingIntent,
                    onConsumeExternalIntent = { incomingIntentState.value = null },
                    onDeliverAssistantResult = { payload ->
                        deliverResultToGoogleAssistant(payload)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentState.value = intent
    }

    override fun onProvideAssistData(data: Bundle?) {
        super.onProvideAssistData(data)
        if (data != null) {
            activeViewModel?.populateAssistBundle(data)
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        if (outContent != null) {
            val vm = activeViewModel
            if (vm != null) {
                outContent.structuredData = vm.buildAssistContentJsonLd()
            }
            outContent.webUri = Uri.parse("vault://query-finance")
        }
    }

    /**
     * Sends relevant financial information and transaction confirmations back to Google Assistant
     * via Activity Result extras and Android's VoiceInteractor API.
     */
    private fun deliverResultToGoogleAssistant(
        payload: FinanceVaultViewModel.AssistantFulfillmentPayload
    ) {
        runCatching {
            val extrasBundle = Bundle().apply {
                putString(Intent.EXTRA_RETURN_RESULT, payload.spokenReply)
                putString("spoken_reply", payload.spokenReply)
                putString("assistant_response", payload.spokenReply)
                putString("net_worth_inr", payload.netWorthFormatted)
                putString("total_balance_inr", payload.totalBalanceFormatted)
                putString("monthly_spending_inr", payload.monthlySpendingFormatted)
                putString("monthly_income_inr", payload.monthlyIncomeFormatted)
                putInt("transactions_recorded", payload.recordedTransactionsCount)
            }
            val resultIntent = Intent().apply {
                putExtras(extrasBundle)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            if (isVoiceInteraction && voiceInteractor != null) {
                val prompt = VoiceInteractor.Prompt(payload.spokenReply)
                voiceInteractor.submitRequest(
                    object : VoiceInteractor.CompleteVoiceRequest(prompt, extrasBundle) {
                        override fun onCompleteResult(result: Bundle?) {
                            super.onCompleteResult(result)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FinanceVaultApp(
    viewModel: FinanceVaultViewModel,
    pendingExternalIntent: Intent? = null,
    onConsumeExternalIntent: () -> Unit = {},
    onDeliverAssistantResult: (FinanceVaultViewModel.AssistantFulfillmentPayload) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val moreDestination by viewModel.moreDestination.collectAsStateWithLifecycle()
    val selectedReportTab by viewModel.selectedReportTab.collectAsStateWithLifecycle()
    val selectedLedgerId by viewModel.selectedLedgerAccountId.collectAsStateWithLifecycle()
    val showAddTxSheet by viewModel.showAddTransactionSheet.collectAsStateWithLifecycle()
    val showGeminiDialog by viewModel.showGeminiAssistantDialog.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val exportPreview by viewModel.exportPreviewResult.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val pendingDisambiguation by viewModel.pendingVoiceDisambiguation.collectAsStateWithLifecycle()
    val reminderState by TransactionRecallReminderManager.state.collectAsStateWithLifecycle()

    val voiceController = remember(context) {
        lateinit var controllerRef: VoiceTransactionController
        controllerRef = VoiceTransactionController(
            context = context,
            onUtteranceCaptured = { spokenText ->
                viewModel.executeVoiceOrGeminiCommand(
                    context = context,
                    utterance = spokenText,
                    onFinished = { reply, err, listenFollowUp ->
                        controllerRef.onCommandProcessedAndSpeak(reply, err, listenFollowUp)
                    }
                )
            },
            onCancelActiveCommand = {
                viewModel.cancelActiveVoiceCommand()
            }
        )
        controllerRef
    }
    val voiceState by voiceController.state.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var pendingStartVoiceAfterPermission by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = audioGranted
        if (audioGranted && pendingStartVoiceAfterPermission) {
            pendingStartVoiceAfterPermission = false
            voiceController.startListeningSession()
        } else {
            pendingStartVoiceAfterPermission = false
        }
    }

    val triggerVoiceCommandOnDemand: () -> Unit = {
        if (hasAudioPermission) {
            voiceController.toggleListeningSession()
        } else {
            pendingStartVoiceAfterPermission = true
            runCatching {
                permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        }
    }

    DisposableEffect(voiceController) {
        onDispose {
            voiceController.destroy()
        }
    }

    // Request notification permission for 6-hour recall reminders if needed (never auto-start microphone on launch)
    LaunchedEffect(uiState.isInitializing, uiState.isLockedByPin) {
        if (!uiState.isInitializing && !uiState.isLockedByPin) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotifPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotifPerm) {
                    runCatching {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            }
        }
    }

    // Handle external Google Assistant / Gemini App Actions and Reminder Notification Intents
    LaunchedEffect(pendingExternalIntent, uiState.isInitializing, uiState.isLockedByPin) {
        if (pendingExternalIntent != null && !uiState.isInitializing && !uiState.isLockedByPin) {
            viewModel.handleExternalGeminiOrVoiceIntent(
                context = context,
                intent = pendingExternalIntent,
                onStartVoiceListening = {
                    triggerVoiceCommandOnDemand()
                },
                onFinishedReply = { reply, err, listenFollowUp ->
                    voiceController.onCommandProcessedAndSpeak(reply, err, listenFollowUp)
                },
                onAssistantFulfillment = { payload ->
                    onDeliverAssistantResult(payload)
                }
            )
            onConsumeExternalIntent()
        }
    }

    // Automatically remove floating pop-up notification after 2 seconds (2000ms)
    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(2000L)
            viewModel.clearStatusBanner()
        }
    }

    // Show loading bar until security settings load so the dashboard never flashes before PIN/Biometrics
    if (uiState.isInitializing) {
        VaultStartupLoadingScreen()
        return
    }

    if (uiState.isLockedByPin) {
        PinLockScreen(
            isBiometricEnabled = uiState.settings.isBiometricEnabled,
            onBiometricUnlockSuccess = { viewModel.unlockWithBiometrics() },
            onUnlockAttempt = { viewModel.attemptPinUnlock(it) }
        )
        return
    }

    // Designed for Pixel 4a (393dp x 851dp, 19.5:9 aspect ratio, top-left punch-hole camera cutout & gesture pill)
    val topBarInsets = WindowInsets.statusBars.union(
        WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnSwipeDown()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = VaultObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultCardSurface)
                        .windowInsetsPadding(topBarInsets)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val searchFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(isSearchExpanded) {
                        if (isSearchExpanded) {
                            runCatching { searchFocusRequester.requestFocus() }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Finance Vault Title & Creator Credit
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Finance Vault",
                                style = MaterialTheme.typography.titleLarge,
                                color = VaultTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("top_bar_app_title")
                            )
                            Text(
                                text = "Made by Lenin and Shaswat",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Statements bar on the left-hand side of the Search bar (Search at top rightmost corner)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Statements button on the left-hand side of Search
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultCardElevated)
                                    .clickable {
                                        viewModel.navigateMore(MoreDestination.REPORTS)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("quick_statements_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = "Statements",
                                        tint = VaultCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Statements",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultCyan,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Top rightmost corner: Small Search bar with Search logo and "Search" label
                            val isSearchActive = isSearchExpanded || searchQuery.isNotBlank()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSearchActive) {
                                            VaultEmerald.copy(alpha = 0.22f)
                                        } else {
                                            VaultCardElevated
                                        }
                                    )
                                    .border(
                                        width = if (isSearchActive) 1.dp else 0.dp,
                                        color = if (isSearchActive) VaultEmerald.copy(alpha = 0.6f) else VaultCardElevated,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleSearchExpanded()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("top_bar_search_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (isSearchActive) VaultEmerald else VaultCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Search",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSearchActive) VaultEmerald else VaultCyan,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    var pendingSearchDeleteTxId by remember { mutableStateOf<Long?>(null) }

                    // Expandable Top Search Bar & Recent History Dropdown (collapses automatically when search input is done)
                    AnimatedVisibility(visible = isSearchExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(isSearchExpanded, searchQuery) {
                                    var totalDragX = 0f
                                    var totalDragY = 0f
                                    detectDragGestures(
                                        onDragStart = {
                                            totalDragX = 0f
                                            totalDragY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y
                                            if (abs(totalDragX) > 16f || abs(totalDragY) > 12f) {
                                                change.consume()
                                                viewModel.collapseSearchBar()
                                                keyboardController?.hide()
                                            }
                                        }
                                    )
                                }
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        "Search accounts, transactions, category, amount, or place...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = VaultEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            viewModel.closeAndResetSearch()
                                            keyboardController?.hide()
                                        },
                                        modifier = Modifier.testTag("top_bar_search_close")
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close Search",
                                            tint = VaultTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.commitSearchQuery(searchQuery)
                                        viewModel.collapseSearchBar()
                                        keyboardController?.hide()
                                    },
                                    onDone = {
                                        viewModel.commitSearchQuery(searchQuery)
                                        viewModel.collapseSearchBar()
                                        keyboardController?.hide()
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .focusRequester(searchFocusRequester)
                                    .testTag("top_bar_search_input")
                            )

                            // Last 5 Search History Chips with Clear option
                            if (searchHistory.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .testTag("top_bar_search_history_section")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent Searches (${searchHistory.size}/5)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VaultTextSecondary
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearSearchHistory() },
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("clear_search_history_button")
                                        ) {
                                            Text(
                                                text = "Clear",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VaultCoral
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        searchHistory.take(5).forEachIndexed { index, term ->
                                            val isCurrentTerm = searchQuery.equals(term, ignoreCase = true)
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        if (isCurrentTerm) VaultEmerald.copy(alpha = 0.22f)
                                                        else VaultCardElevated
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isCurrentTerm) VaultEmerald else VaultCyan.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.selectSearchHistoryTerm(term)
                                                        viewModel.collapseSearchBar()
                                                        keyboardController?.hide()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                                    .testTag("search_history_chip_$index"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = if (isCurrentTerm) VaultEmerald else VaultCyan,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = term,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VaultTextPrimary,
                                                    maxLines = 1
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove $term",
                                                    tint = VaultTextSecondary,
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .clickable { viewModel.removeSearchHistoryTerm(term) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Persistent Search Results & Active Query Banner (stays visible after search bar auto-closes until navigation)
                    if (searchQuery.isNotBlank()) {
                        val matchedAccounts = remember(uiState.accountsWithBalances, searchQuery) {
                            val q = searchQuery.trim()
                            uiState.accountsWithBalances.filter {
                                it.account.name.contains(q, ignoreCase = true) ||
                                    it.account.bankName.contains(q, ignoreCase = true) ||
                                    it.account.accountType.displayName.contains(q, ignoreCase = true)
                            }.take(3)
                        }
                        val quickMatches = remember(uiState.transactions, searchQuery) {
                            uiState.transactions.filter { it.matchesSearchQuery(searchQuery) }.take(4)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isSearchExpanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(VaultEmerald.copy(alpha = 0.14f))
                                        .border(1.dp, VaultEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("active_search_filter_banner"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.toggleSearchExpanded() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = VaultEmerald,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Results for \"$searchQuery\"",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VaultTextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.closeAndResetSearch() },
                                        modifier = Modifier
                                            .height(26.dp)
                                            .testTag("clear_active_search_button")
                                    ) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VaultCoral,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (matchedAccounts.isNotEmpty()) {
                                Text(
                                    text = "Matching Accounts (${matchedAccounts.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultCyan
                                )
                                matchedAccounts.forEach { accWithBal ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(VaultCardElevated)
                                            .clickable {
                                                keyboardController?.hide()
                                                val ledgerId = accWithBal.ledgerAccount?.id
                                                if (ledgerId != null) {
                                                    viewModel.selectLedgerAccount(ledgerId)
                                                    viewModel.selectReportTab(ReportTab.ACCOUNT_LEDGER)
                                                    viewModel.navigateMore(MoreDestination.REPORTS)
                                                } else {
                                                    viewModel.selectTab(MainTab.ACCOUNTS)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${accWithBal.account.name} (${accWithBal.account.accountType.displayName})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VaultTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = InrMoney.formatPaise(
                                                accWithBal.closingBalancePaise,
                                                isHidden = uiState.settings.isPrivacyModeEnabled
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VaultEmerald
                                        )
                                    }
                                }
                            }

                            if (selectedTab != MainTab.HOME && selectedTab != MainTab.ACTIVITY) {
                                Text(
                                    text = if (quickMatches.isEmpty() && matchedAccounts.isEmpty()) {
                                        "No results matching \"$searchQuery\""
                                    } else if (quickMatches.isNotEmpty()) {
                                        "Matching Transactions (${quickMatches.size} shown):"
                                    } else {
                                        ""
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultEmerald
                                )
                                quickMatches.forEach { tx ->
                                    Box(
                                        modifier = Modifier.clickable {
                                            keyboardController?.hide()
                                            viewModel.collapseSearchBar()
                                        }
                                    ) {
                                        TransactionRowCard(
                                            tx = tx,
                                            isHidden = uiState.settings.isPrivacyModeEnabled,
                                            onReverseClick = {
                                                pendingSearchDeleteTxId = tx.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    pendingSearchDeleteTxId?.let { txId ->
                        ConfirmDeleteTransactionsDialog(
                            deleteCount = 1,
                            onConfirmDelete = {
                                viewModel.reverseTransaction(txId)
                                pendingSearchDeleteTxId = null
                            },
                            onDismiss = { pendingSearchDeleteTxId = null }
                        )
                    }
                }
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Right-side Floating Microphone / Mute Icon Button (Above Bottom Menu)
                    // Waits 4 seconds before & after speech then auto-closes; tapping while active immediately mutes & cancels
                    val isVoiceBusy = voiceState.isBusyOrActive
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = isVoiceBusy
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = VaultCardElevated,
                                tonalElevation = 8.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (voiceState.isActivelyListening) VaultCoral else VaultEmerald,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { voiceController.stopAndMuteAll() }
                                    .testTag("floating_voice_listening_pill")
                            ) {
                                Text(
                                    text = when {
                                        voiceState.isProcessingCommand -> "Processing... Tap to cancel"
                                        voiceState.isSpeakingReply -> "Speaking... Tap to mute"
                                        voiceState.partialTranscript.isNotBlank() ->
                                            "${voiceState.partialTranscript} (${voiceState.remainingSilenceSeconds}s)"
                                        else -> "Listening (${voiceState.remainingSilenceSeconds}s)... Speak now"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (voiceState.isActivelyListening) VaultEmerald else VaultCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .widthIn(max = 225.dp)
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                if (isVoiceBusy) {
                                    voiceController.stopAndMuteAll()
                                } else {
                                    triggerVoiceCommandOnDemand()
                                }
                            },
                            shape = CircleShape,
                            containerColor = if (isVoiceBusy) VaultCoral else VaultCardElevated,
                            contentColor = if (isVoiceBusy) VaultObsidianBg else VaultEmerald,
                            modifier = Modifier
                                .size(52.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isVoiceBusy) VaultCoral else VaultEmerald,
                                    shape = CircleShape
                                )
                                .testTag("fab_voice_command")
                        ) {
                            Icon(
                                imageVector = if (isVoiceBusy) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isVoiceBusy) "Mute & Stop Voice Command" else "Voice Command Microphone",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openAddTransactionSheet(true) },
                        containerColor = VaultEmerald,
                        contentColor = VaultObsidianBg,
                        icon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Record Transaction",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                "Record",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("fab_record_transaction")
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = VaultCardSurface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    MainTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            MainTab.HOME -> Icons.Default.Home
                            MainTab.ACCOUNTS -> Icons.Default.AccountBalanceWallet
                            MainTab.ACTIVITY -> Icons.Default.ReceiptLong
                            MainTab.INVESTMENTS -> Icons.Default.TrendingUp
                            MainTab.MORE -> Icons.Default.Menu
                        }
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VaultObsidianBg,
                                selectedTextColor = VaultEmerald,
                                indicatorColor = VaultEmerald,
                                unselectedIconColor = VaultTextSecondary,
                                unselectedTextColor = VaultTextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(isSearchExpanded) {
                        if (isSearchExpanded) {
                            awaitEachGesture {
                                awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                viewModel.collapseSearchBar()
                                keyboardController?.hide()
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Centered container optimized for Pixel 4a (393dp width) and adaptable up to 440dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 440.dp)
                ) {
                    when (selectedTab) {
                        MainTab.HOME -> {
                            DashboardScreen(
                                uiState = uiState,
                                searchQuery = searchQuery,
                                onTogglePrivacy = { viewModel.togglePrivacyMode() },
                                onOpenAddTransaction = { viewModel.openAddTransactionSheet(true) },
                                onNavigateTab = { viewModel.selectTab(it) },
                                onNavigateMore = { viewModel.navigateMore(it) },
                                onToggleDemoMode = { viewModel.toggleDemoMode(it) },
                                onReverseTransaction = { viewModel.reverseTransaction(it) },
                                onBulkReverseTransactions = { viewModel.reverseTransactionsBulk(it) },
                                onBulkCategorizeTransactions = { ids, cat -> viewModel.categorizeTransactionsBulk(ids, cat) },
                                onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) },
                                topVoiceAndRecallBanner = if (reminderState.isInAppRecallDue) {
                                    {
                                        HandsFreeVoiceAndRecallSection(
                                            voiceState = voiceState,
                                            reminderState = reminderState,
                                            hasAudioPermission = hasAudioPermission,
                                            onRequestAudioPermission = { triggerVoiceCommandOnDemand() },
                                            onToggleVoiceCommand = { triggerVoiceCommandOnDemand() },
                                            onOpenGeminiAssistant = { viewModel.openGeminiAssistantDialog(true) },
                                            onOpenReminderSettings = {
                                                viewModel.navigateMore(MoreDestination.SETTINGS)
                                            },
                                            onDismissRecallBanner = {
                                                TransactionRecallReminderManager.dismissInAppRecallPrompt(context)
                                            }
                                        )
                                    }
                                } else null
                            )
                        }

                        MainTab.ACCOUNTS -> {
                            AccountsScreen(
                                uiState = uiState,
                                onCreateAccount = { name, type, bank, branch, accNo, ifsc, holder, upi, openPaise, isOpenRec, limit, notes ->
                                    viewModel.createAccount(name, type, bank, branch, accNo, ifsc, holder, upi, openPaise, isOpenRec, limit, notes)
                                },
                                onArchiveAccount = { id, arch -> viewModel.archiveAccount(id, arch) },
                                onOpenAccountLedger = { ledgerId ->
                                    viewModel.selectLedgerAccount(ledgerId)
                                    viewModel.selectReportTab(ReportTab.ACCOUNT_LEDGER)
                                    viewModel.navigateMore(MoreDestination.REPORTS)
                                },
                                onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) }
                            )
                        }

                        MainTab.ACTIVITY -> {
                            ActivityScreen(
                                uiState = uiState,
                                searchQuery = searchQuery,
                                onOpenAddTransaction = { viewModel.openAddTransactionSheet(true) },
                                onReverseTransaction = { viewModel.reverseTransaction(it) },
                                onBulkReverseTransactions = { viewModel.reverseTransactionsBulk(it) },
                                onBulkCategorizeTransactions = { ids, cat -> viewModel.categorizeTransactionsBulk(ids, cat) },
                                onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) }
                            )
                        }

                        MainTab.INVESTMENTS -> {
                            InvestmentsScreen(
                                uiState = uiState,
                                onAddInvestment = { name, cat, date, qty, units, price, invested, charges, curVal, isCurRec, payAcc, rate, comp, dur, notes ->
                                    viewModel.addInvestment(name, cat, date, qty, units, price, invested, charges, curVal, isCurRec, payAcc, rate, comp, dur, notes)
                                },
                                onUpdateRecordedValuation = { id, valPaise ->
                                    viewModel.updateInvestmentValuation(id, valPaise)
                                },
                                onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) }
                            )
                        }

                        MainTab.MORE -> {
                            when (moreDestination) {
                                MoreDestination.HUB -> {
                                    MoreHubScreen(
                                        uiState = uiState,
                                        onSelectDestination = { viewModel.navigateMore(it) }
                                    )
                                }

                                MoreDestination.REPORTS -> {
                                    FinancialReportsScreen(
                                        uiState = uiState,
                                        selectedReportTab = selectedReportTab,
                                        selectedLedgerAccountId = selectedLedgerId,
                                        onSelectReportTab = { viewModel.selectReportTab(it) },
                                        onSelectLedgerAccount = { viewModel.selectLedgerAccount(it) },
                                        onExportPdf = { viewModel.preparePdfExport(context) },
                                        onExportExcel = { viewModel.prepareCsvOrExcelExport(context, asExcel = true) },
                                        onExportCsv = { viewModel.prepareCsvOrExcelExport(context, asExcel = false) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.ANALYTICS -> {
                                    AnalyticsScreen(
                                        uiState = uiState,
                                        onOpenAddTransaction = { viewModel.openAddTransactionSheet(true) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.LOANS -> {
                                    LoansAndCreditCardsScreen(
                                        uiState = uiState,
                                        onAddLoan = { name, type, lender, p, r, n, start, accId, notes ->
                                            viewModel.addLoan(name, type, lender, p, r, n, start, accId, notes)
                                        },
                                        onRecordEmiPayment = { loanId, srcAccId, totalEmi, prin, interest, loanName ->
                                            viewModel.recordTransaction(
                                                transactionType = TransactionType.LOAN_REPAYMENT,
                                                dateEpochDay = IndianFinancialYear.todayEpochDay(),
                                                amountPaise = totalEmi,
                                                principalComponentPaise = prin,
                                                interestComponentPaise = interest,
                                                sourceAccountId = srcAccId,
                                                category = "Loan EMI Repayment",
                                                description = "EMI Repayment for $loanName",
                                                linkedLoanId = loanId
                                            )
                                        },
                                        onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.BUDGETS_GOALS -> {
                                    BudgetsAndGoalsScreen(
                                        uiState = uiState,
                                        onAddBudget = { name, period, cat, lim, notes ->
                                            viewModel.addBudget(name, period, cat, lim, notes)
                                        },
                                        onSaveOverallBudget = { limitPaise ->
                                            viewModel.saveOverallMonthlyBudget(limitPaise)
                                        },
                                        onDeleteBudget = { viewModel.deleteBudget(it) },
                                        onAddGoal = { name, type, tgt, cur, notes ->
                                            viewModel.addGoal(name, type, tgt, cur, notes)
                                        },
                                        onContributeGoal = { id, amt -> viewModel.contributeToGoal(id, amt) },
                                        onDeleteGoal = { viewModel.deleteGoal(it) },
                                        onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.GIFT_CARDS -> {
                                    GiftCardsScreen(
                                        uiState = uiState,
                                        onAddGiftCard = { brand, name, initP, remP, cardNo, pin, accId, notes ->
                                            viewModel.addGiftCard(brand, name, initP, remP, cardNo, pin, accId, notes)
                                        },
                                        onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.SUBSCRIPTIONS -> {
                                    SubscriptionsScreen(
                                        uiState = uiState,
                                        onAddSubscription = { name, cost, freq, nextRen, accId, notes, count, unit ->
                                            viewModel.addSubscription(name, cost, freq, nextRen, accId, notes, count, unit)
                                        },
                                        onUpdateRenewalSchedule = { subId, nextRen, freq, count, unit ->
                                            viewModel.updateSubscriptionRenewalSchedule(subId, nextRen, freq, count, unit)
                                        },
                                        onRecordPaymentNow = { subId, accId, customNextEpoch ->
                                            viewModel.recordSubscriptionPaymentNow(subId, accId, customNextEpoch)
                                        },
                                        onDeleteSubscription = { viewModel.deleteSubscription(it) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.TAXES -> {
                                    TaxesScreen(
                                        uiState = uiState,
                                        onAddTaxRecord = { type, fy, amt, challan, accId, doc, notes ->
                                            viewModel.addTaxRecord(type, fy, amt, challan, accId, doc, notes)
                                        },
                                        onAddCustomOption = { scope, value -> viewModel.addCustomOption(scope, value) },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }

                                MoreDestination.SETTINGS -> {
                                    SettingsAndAboutScreen(
                                        uiState = uiState,
                                        onUpdateProfileAndFy = { name, pan, fy ->
                                            viewModel.updateProfileAndFy(name, pan, fy)
                                        },
                                        onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                                        onUpdatePin = { viewModel.updateSecurityPin(it) },
                                        onVerifyOldPin = { viewModel.verifyCurrentPin(it) },
                                        onToggleBiometric = { viewModel.toggleBiometricLock(it) },
                                        onLockNow = { viewModel.lockAppNow() },
                                        onTogglePrivacy = { viewModel.togglePrivacyMode() },
                                        onToggleDemoMode = { viewModel.toggleDemoMode(it) },
                                        onExportJsonBackup = { viewModel.prepareJsonBackupExport(context) },
                                        onRestoreJsonBackup = { viewModel.restoreFromJsonBackup(it) },
                                        onClearData = { viewModel.clearCurrentModeData() },
                                        onBack = { viewModel.navigateMore(MoreDestination.HUB) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating 2-Second Pop-Up Notification Overlay (Never pushes down the main window; supports swipe Up, Down, Left, or Right to dismiss)
        AnimatedVisibility(
            visible = statusBanner != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(topBarInsets)
                .padding(top = 10.dp, start = 14.dp, end = 14.dp)
                .widthIn(max = 420.dp)
                .zIndex(25f)
        ) {
            statusBanner?.let { msg ->
                var dragOffsetX by remember(msg) { mutableFloatStateOf(0f) }
                var dragOffsetY by remember(msg) { mutableFloatStateOf(0f) }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VaultCardElevated,
                    tonalElevation = 10.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                        }
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .border(1.dp, VaultEmerald.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .pointerInput(msg) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (abs(dragOffsetX) > 10f || abs(dragOffsetY) > 8f) {
                                        viewModel.clearStatusBanner()
                                    } else {
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragCancel = {
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y
                                    // Dismiss immediately on upward, downward, rightward, or leftward swipe
                                    if (abs(dragAmount.x) > 4f || abs(dragAmount.y) > 4f || abs(dragOffsetX) > 14f || abs(dragOffsetY) > 12f) {
                                        viewModel.clearStatusBanner()
                                    }
                                }
                            )
                        }
                        .testTag("floating_popup_notification")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VaultEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultTextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = VaultTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Swipe any direction to dismiss",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { viewModel.clearStatusBanner() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = VaultTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Multi-Option Voice Disambiguation Overlay Card (when a command has multiple options)
        AnimatedVisibility(
            visible = pendingDisambiguation != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 92.dp, start = 14.dp, end = 80.dp)
                .widthIn(max = 360.dp)
                .zIndex(24f)
        ) {
            pendingDisambiguation?.let { disambig ->
                VoiceDisambiguationOverlayCard(
                    disambiguation = disambig,
                    isActivelyListening = voiceState.isActivelyListening,
                    partialTranscript = voiceState.partialTranscript,
                    onSelectOption = { chosenOption ->
                        voiceController.stopListeningSession()
                        viewModel.selectVoiceDisambiguationOption(
                            option = chosenOption,
                            onFinished = { reply, err, listenFollowUp ->
                                voiceController.onCommandProcessedAndSpeak(reply, err, listenFollowUp)
                            }
                        )
                    },
                    onListenAgain = { triggerVoiceCommandOnDemand() },
                    onCancel = {
                        voiceController.stopListeningSession()
                        viewModel.dismissVoiceDisambiguation()
                    }
                )
            }
        }
    }

    if (showAddTxSheet) {
        RecordTransactionDialog(
            accounts = uiState.accountsWithBalances,
            investments = uiState.investments,
            loans = uiState.loans,
            giftCards = uiState.giftCards,
            transactions = uiState.transactions,
            customCategories = uiState.customOptionsMap["TX_CATEGORY"].orEmpty(),
            onAddCustomCategory = { viewModel.addCustomOption("TX_CATEGORY", it) },
            onDismiss = { viewModel.openAddTransactionSheet(false) },
            onSubmit = { type, date, amt, prin, interest, charges, costDisp, srcId, dstId, cat, desc, ref, notes, locName, lat, lng, invId, loanId, gcId, origTxId ->
                viewModel.recordTransaction(
                    transactionType = type,
                    dateEpochDay = date,
                    amountPaise = amt,
                    principalComponentPaise = prin,
                    interestComponentPaise = interest,
                    chargesPaise = charges,
                    costBasisDisposedPaise = costDisp,
                    sourceAccountId = srcId,
                    destinationAccountId = dstId,
                    category = cat,
                    description = desc,
                    referenceNumber = ref,
                    notes = notes,
                    locationName = locName,
                    latitude = lat,
                    longitude = lng,
                    linkedInvestmentId = invId,
                    linkedLoanId = loanId,
                    linkedGiftCardId = gcId,
                    linkedOriginalTransactionId = origTxId
                )
            }
        )
    }

    if (exportPreview != null) {
        ExportPreviewDialog(
            result = exportPreview!!,
            onDismiss = { viewModel.dismissExportPreview() }
        )
    }

    if (showGeminiDialog) {
        GeminiVoiceAssistantDialog(
            voiceState = voiceState,
            reminderState = reminderState,
            hasAudioPermission = hasAudioPermission,
            onRequestAudioPermission = { triggerVoiceCommandOnDemand() },
            onToggleVoiceCommand = { triggerVoiceCommandOnDemand() },
            onSendCommandText = { textCommand ->
                voiceController.markProcessingStarted(textCommand)
                viewModel.executeVoiceOrGeminiCommand(
                    context = context,
                    utterance = textCommand,
                    onFinished = { reply, err, listenFollowUp ->
                        voiceController.onCommandProcessedAndSpeak(reply, err, listenFollowUp)
                    }
                )
            },
            onOpenReminderSettings = {
                viewModel.navigateMore(MoreDestination.SETTINGS)
            },
            onDismiss = { viewModel.openGeminiAssistantDialog(false) }
        )
    }
}
