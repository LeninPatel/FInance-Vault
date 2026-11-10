package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ai.VoiceAssistantUiState
import com.example.domain.reminder.ReminderSettingsState
import com.example.domain.reminder.TransactionRecallReminderManager
import com.example.ui.FinanceVaultViewModel
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

@Composable
fun HandsFreeVoiceAndRecallSection(
    voiceState: VoiceAssistantUiState,
    reminderState: ReminderSettingsState,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onToggleVoiceCommand: () -> Unit,
    onOpenGeminiAssistant: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onDismissRecallBanner: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. 6-Hour / Custom Schedule Transaction Recall Reminder Banner (when due)
        if (reminderState.isInAppRecallDue) {
            VaultGlassCard(
                accentColor = VaultGold,
                modifier = Modifier.testTag("in_app_transaction_recall_banner")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = VaultGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminderState.activeRecallLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VaultGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Time to recall & log your recent transactions. Press Voice Command to speak!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultTextPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismissRecallBanner,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dismiss_recall_banner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Recall Reminder",
                                tint = VaultTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasAudioPermission) onToggleVoiceCommand() else onRequestAudioPermission()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("recall_banner_speak_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = VaultObsidianBg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Speak Transaction",
                                style = MaterialTheme.typography.labelMedium,
                                color = VaultObsidianBg,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenReminderSettings,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("recall_banner_manage_schedules_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = VaultCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custom Timings (${reminderState.schedules.count { it.isEnabled }})",
                                style = MaterialTheme.typography.labelMedium,
                                color = VaultCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceDisambiguationOverlayCard(
    disambiguation: FinanceVaultViewModel.PendingVoiceDisambiguation,
    isActivelyListening: Boolean,
    partialTranscript: String,
    onSelectOption: (FinanceVaultViewModel.VoiceDisambiguationOption) -> Unit,
    onListenAgain: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    VaultGlassCard(
        accentColor = VaultGold,
        modifier = modifier.testTag("voice_disambiguation_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = VaultGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = disambiguation.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = VaultGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isActivelyListening && partialTranscript.isNotBlank()) {
                                "Hearing: \"$partialTranscript\""
                            } else if (isActivelyListening) {
                                "Speak option name or number (1, 2...), or tap below:"
                            } else {
                                "Choose an option below or tap Mic to speak:"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActivelyListening) VaultEmerald else VaultTextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("dismiss_voice_disambiguation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Selection",
                        tint = VaultTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            disambiguation.options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultObsidianBg)
                        .border(1.dp, VaultEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onSelectOption(option) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("voice_disambiguation_option_${option.index}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(VaultEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${option.index}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (option.subtitle.isNotBlank()) {
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultCyan,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onListenAgain) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = VaultEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speak Choice", color = VaultEmerald, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = VaultCoral, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun GeminiVoiceAssistantDialog(
    voiceState: VoiceAssistantUiState,
    reminderState: ReminderSettingsState,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onToggleVoiceCommand: () -> Unit,
    onSendCommandText: (String) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var commandInput by remember { mutableStateOf("") }

    val samplePrompts = listOf(
        "Show me my Balance Sheet",
        "Open Subscriptions",
        "Spent 450 rupees on coffee at Starbucks",
        "Create a bank account SBI Savings with 25000",
        "Add Netflix subscription for 649 every month",
        "Set a grocery budget of 12000",
        "Search for groceries",
        "Undo last action"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = VaultEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gemini & Voice Command",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Press Voice Command to speak in English (Battery-friendly)",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultEmerald
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // On-Demand Voice Command Button inside Dialog
                val isVoiceBusy = voiceState.isBusyOrActive
                Button(
                    onClick = {
                        if (!hasAudioPermission) {
                            onRequestAudioPermission()
                        } else {
                            onToggleVoiceCommand()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVoiceBusy) VaultCoral else VaultEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_dialog_voice_command_button")
                ) {
                    Icon(
                        imageVector = if (isVoiceBusy) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = VaultObsidianBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            voiceState.isActivelyListening ->
                                "Listening (${voiceState.remainingSilenceSeconds}s)... Tap to Stop & Mute"
                            voiceState.isProcessingCommand ->
                                "Processing... Tap to Cancel & Mute"
                            voiceState.isSpeakingReply ->
                                "Speaking... Tap to Mute"
                            else ->
                                "Press to Speak Voice Command"
                        },
                        color = VaultObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live Transcript & Gemini Response Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultCardElevated)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (voiceState.isActivelyListening) VaultEmerald else VaultCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                voiceState.isActivelyListening && voiceState.partialTranscript.isNotBlank() ->
                                    "Hearing: \"${voiceState.partialTranscript}\" (auto-closes after ${voiceState.remainingSilenceSeconds}s silence)"
                                voiceState.isActivelyListening ->
                                    "Microphone active (${voiceState.remainingSilenceSeconds}s grace window) — speak your command now..."
                                voiceState.isProcessingCommand ->
                                    "Processing: \"${voiceState.lastSpokenTranscript}\"..."
                                voiceState.lastSpokenTranscript.isNotBlank() ->
                                    "Last Heard: \"${voiceState.lastSpokenTranscript}\""
                                else ->
                                    "Microphone is off to save battery. Press the button above to speak."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (voiceState.lastGeminiReply.isNotBlank()) {
                        HorizontalDivider(color = VaultObsidianBg)
                        Text(
                            text = "Gemini Response:",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultEmerald
                        )
                        Text(
                            text = voiceState.lastGeminiReply,
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextPrimary
                        )
                    }

                    if (!voiceState.lastError.isNullOrBlank()) {
                        Text(
                            text = voiceState.lastError,
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultCoral
                        )
                    }
                }

                // Quick Text Input for Gemini
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    label = { Text("Or type a command / question for Gemini") },
                    placeholder = { Text("e.g., Spent 350 rupees on lunch at Subway") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    keyboardController?.hide()
                                    onSendCommandText(commandInput.trim())
                                    commandInput = ""
                                }
                            },
                            modifier = Modifier.testTag("gemini_send_command_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send to Gemini", tint = VaultEmerald)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandInput.isNotBlank()) {
                                keyboardController?.hide()
                                onSendCommandText(commandInput.trim())
                                commandInput = ""
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_command_text_input")
                )

                Text(
                    text = "Try saying or tapping an example:",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultTextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samplePrompts.forEachIndexed { idx, sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(VaultObsidianBg)
                                .border(1.dp, VaultCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable {
                                    onSendCommandText(sample)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("gemini_sample_chip_$idx")
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultCyan,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Device Google Assistant & Gemini Bidirectional Integration Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultObsidianBg)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Google Assistant & Gemini Integration Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = VaultGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• When you request financial info or record a transaction via Google Assistant, Finance Vault returns your live balances, net worth, spending, and confirmation back to Google Assistant and speaks it aloud.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                    Text(
                        text = "• Tap the floating microphone icon on the right side above the bottom menu anytime to speak a command (zero idle battery drain).",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultCyan
                    )
                    Text(
                        text = "• Active Recall Reminders: ${reminderState.schedules.count { it.isEnabled }} schedule(s) configured.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultEmerald,
                        modifier = Modifier.clickable {
                            onDismiss()
                            onOpenReminderSettings()
                        }
                    )
                }
            }
        },
        confirmButton = {
            val isVoiceBusy = voiceState.isBusyOrActive
            Button(
                onClick = {
                    if (hasAudioPermission) onToggleVoiceCommand() else onRequestAudioPermission()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVoiceBusy) VaultCoral else VaultEmerald
                ),
                modifier = Modifier.testTag("gemini_listen_now_button")
            ) {
                Icon(
                    imageVector = if (isVoiceBusy) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = VaultObsidianBg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isVoiceBusy) "Stop & Mute" else "Voice Command",
                    color = VaultObsidianBg,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CustomRecallRemindersManagementCard(
    reminderState: ReminderSettingsState,
    onRequestNotificationPermissionIfNeeded: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    VaultGlassCard(
        accentColor = VaultGold,
        modifier = Modifier.testTag("settings_recall_reminders_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = VaultGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Transaction Recall Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = "6-hour auto-reminder & multiple custom notification timings",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextSecondary
                        )
                    }
                }
                Button(
                    onClick = {
                        onRequestNotificationPermissionIfNeeded()
                        showAddScheduleDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_reminder_time_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = VaultObsidianBg,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = VaultObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Active & Custom Time Period Schedules List
            reminderState.schedules.forEach { schedule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultObsidianBg)
                        .border(
                            width = 1.dp,
                            color = if (schedule.isEnabled) VaultEmerald.copy(alpha = 0.4f) else VaultCardElevated,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("reminder_schedule_row_${schedule.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (schedule.isEnabled) VaultEmerald else VaultTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = schedule.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VaultTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = schedule.timingSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (schedule.isEnabled) VaultCyan else VaultTextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VaultToggleSwitch(
                            checked = schedule.isEnabled,
                            onCheckedChange = { enabled ->
                                onRequestNotificationPermissionIfNeeded()
                                TransactionRecallReminderManager.toggleSchedule(context, schedule.id, enabled)
                            }
                        )
                        if (schedule.id != "default_every_6_hours") {
                            IconButton(
                                onClick = {
                                    TransactionRecallReminderManager.deleteSchedule(context, schedule.id)
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("delete_reminder_schedule_${schedule.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Custom Time Period",
                                    tint = VaultCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = VaultCardElevated)

            // Quiet Hours Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quiet Hours (${String.format("%02d:00", reminderState.quietStartHour)} – ${String.format("%02d:00", reminderState.quietEndHour)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Mute overnight system notifications while keeping in-app recall prompts",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                }
                VaultToggleSwitch(
                    checked = reminderState.quietHoursEnabled,
                    onCheckedChange = {
                        TransactionRecallReminderManager.updateQuietHours(context, it)
                    },
                    modifier = Modifier.testTag("quiet_hours_switch")
                )
            }

            OutlinedButton(
                onClick = {
                    onRequestNotificationPermissionIfNeeded()
                    TransactionRecallReminderManager.triggerImmediateTestNotification(
                        context,
                        "Transaction Recall Check"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_recall_notification_button")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Trigger Recall Reminder Notification Now", color = VaultGold)
            }
        }
    }

    if (showAddScheduleDialog) {
        AddCustomReminderScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onSaveSchedule = { label, mode, intervalHours, intervalMinutes, hourOfDay, minuteOfDay ->
                TransactionRecallReminderManager.addCustomSchedule(
                    context = context,
                    label = label,
                    mode = mode,
                    intervalHours = intervalHours,
                    intervalMinutes = intervalMinutes,
                    hourOfDay = hourOfDay,
                    minuteOfDay = minuteOfDay
                )
                showAddScheduleDialog = false
            }
        )
    }
}

@Composable
fun AddCustomReminderScheduleDialog(
    onDismiss: () -> Unit,
    onSaveSchedule: (
        label: String,
        mode: String,
        intervalHours: Int,
        intervalMinutes: Int,
        hourOfDay: Int,
        minuteOfDay: Int
    ) -> Unit
) {
    var mode by remember { mutableStateOf("DAILY_TIME") } // "DAILY_TIME" or "INTERVAL"
    var label by remember { mutableStateOf("") }
    var selectedHour by remember { mutableIntStateOf(20) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var intervalHoursInput by remember { mutableStateOf("6") }
    var intervalMinutesInput by remember { mutableStateOf("0") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dailyPresets = listOf(
        Triple("Morning Recall", 9, 0),
        Triple("Afternoon Recall", 14, 30),
        Triple("Evening Recall", 19, 0),
        Triple("Night Recall", 21, 30)
    )
    val intervalPresets = listOf(
        Triple("Every 2 Hours", 2, 0),
        Triple("Every 4 Hours", 4, 0),
        Triple("Every 6 Hours", 6, 0),
        Triple("Every 8 Hours", 8, 0),
        Triple("Every 12 Hours", 12, 0)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Text(
                text = "Add Custom Reminder Time Period",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultOptionTogglePill(
                        selected = mode == "DAILY_TIME",
                        label = "Daily Time Slot",
                        leadingIcon = Icons.Default.AccessTime,
                        onClick = { mode = "DAILY_TIME"; errorMsg = null },
                        modifier = Modifier.weight(1f)
                    )
                    VaultOptionTogglePill(
                        selected = mode == "INTERVAL",
                        label = "Custom Interval",
                        leadingIcon = Icons.Default.Schedule,
                        onClick = { mode = "INTERVAL"; errorMsg = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (mode == "DAILY_TIME") {
                    Text(
                        text = "Quick Daily Time Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dailyPresets.forEach { (presetLabel, h, m) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedHour == h && selectedMinute == m) VaultEmerald.copy(alpha = 0.22f)
                                        else VaultObsidianBg
                                    )
                                    .border(
                                        1.dp,
                                        if (selectedHour == h && selectedMinute == m) VaultEmerald else VaultCyan.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        label = presetLabel
                                        selectedHour = h
                                        selectedMinute = m
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$presetLabel (${String.format("%02d:%02d", h, m)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextPrimary
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = selectedHour.toString(),
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(2).toIntOrNull() ?: 0
                                selectedHour = v.coerceIn(0, 23)
                            },
                            label = { Text("Hour (0–23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_reminder_hour_input")
                        )
                        OutlinedTextField(
                            value = selectedMinute.toString(),
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(2).toIntOrNull() ?: 0
                                selectedMinute = v.coerceIn(0, 59)
                            },
                            label = { Text("Minute (0–59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_reminder_minute_input")
                        )
                    }
                } else {
                    Text(
                        text = "Quick Repeating Interval Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultTextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervalPresets.forEach { (presetLabel, h, m) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VaultObsidianBg)
                                    .border(1.dp, VaultCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        label = presetLabel
                                        intervalHoursInput = h.toString()
                                        intervalMinutesInput = m.toString()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = presetLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultTextPrimary
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = intervalHoursInput,
                            onValueChange = {
                                intervalHoursInput = it.filter { c -> c.isDigit() }.take(2)
                                errorMsg = null
                            },
                            label = { Text("Every Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_reminder_interval_hours_input")
                        )
                        OutlinedTextField(
                            value = intervalMinutesInput,
                            onValueChange = {
                                intervalMinutesInput = it.filter { c -> c.isDigit() }.take(2)
                                errorMsg = null
                            },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_reminder_interval_minutes_input")
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Reminder Label (Optional)") },
                    placeholder = {
                        Text(
                            if (mode == "DAILY_TIME") "e.g., Evening Expense Recall"
                            else "e.g., Every 4 Hours Recall"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_reminder_label_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultCoral
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = intervalHoursInput.toIntOrNull() ?: 0
                    val m = intervalMinutesInput.toIntOrNull() ?: 0
                    if (mode == "INTERVAL" && h <= 0 && m <= 0) {
                        errorMsg = "Please enter an interval greater than 0 minutes."
                        return@Button
                    }
                    val finalLabel = label.trim().ifBlank {
                        if (mode == "DAILY_TIME") {
                            String.format("Daily %02d:%02d Recall", selectedHour, selectedMinute)
                        } else {
                            if (h > 0 && m > 0) "Every ${h}h ${m}m Recall"
                            else if (h > 0) "Every $h Hours Recall"
                            else "Every $m Minutes Recall"
                        }
                    }
                    onSaveSchedule(finalLabel, mode, h, m, selectedHour, selectedMinute)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                modifier = Modifier.testTag("save_custom_reminder_schedule_button")
            ) {
                Text("Save Time Period", color = VaultObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
