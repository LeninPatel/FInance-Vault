package com.example.domain.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Immutable
data class RecallReminderSchedule(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val mode: String, // "INTERVAL" or "DAILY_TIME"
    val intervalHours: Int = 6,
    val intervalMinutes: Int = 0,
    val hourOfDay: Int = 20,
    val minuteOfDay: Int = 0,
    val isEnabled: Boolean = true
) {
    fun timingSummary(): String {
        return if (mode == "DAILY_TIME") {
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val hour12 = when {
                hourOfDay == 0 -> 12
                hourOfDay > 12 -> hourOfDay - 12
                else -> hourOfDay
            }
            String.format(Locale.US, "Daily at %02d:%02d %s", hour12, minuteOfDay, amPm)
        } else {
            val h = intervalHours.coerceAtLeast(0)
            val m = intervalMinutes.coerceAtLeast(0)
            when {
                h > 0 && m > 0 -> "Every ${h}h ${m}m"
                h > 0 -> if (h == 1) "Every 1 Hour" else "Every $h Hours"
                m > 0 -> "Every $m Minutes"
                else -> "Every 6 Hours"
            }
        }
    }

    fun intervalMillis(): Long {
        val totalMinutes = (intervalHours.coerceAtLeast(0) * 60L + intervalMinutes.coerceAtLeast(0)).coerceAtLeast(15L)
        return totalMinutes * 60_000L
    }
}

@Immutable
data class ReminderSettingsState(
    val schedules: List<RecallReminderSchedule> = emptyList(),
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val isInAppRecallDue: Boolean = false,
    val activeRecallLabel: String = "6-Hour Transaction Recall"
)

object TransactionRecallReminderManager {
    const val ACTION_TRANSACTION_RECALL_REMINDER = "com.example.ACTION_TRANSACTION_RECALL_REMINDER"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_SCHEDULE_LABEL = "extra_schedule_label"
    const val EXTRA_START_VOICE_RECORD = "extra_start_voice_record"
    const val EXTRA_OPEN_RECORD_SHEET = "extra_open_record_sheet"

    private const val PREFS_NAME = "finance_vault_recall_reminders"
    private const val KEY_SCHEDULES_JSON = "schedules_json"
    private const val KEY_QUIET_ENABLED = "quiet_hours_enabled"
    private const val KEY_QUIET_START = "quiet_start_hour"
    private const val KEY_QUIET_END = "quiet_end_hour"
    private const val KEY_LAST_PROMPT_MILLIS = "last_recall_prompt_millis"
    private const val KEY_IN_APP_DUE = "in_app_recall_due"
    private const val KEY_ACTIVE_RECALL_LABEL = "active_recall_label"
    private const val CHANNEL_ID = "finance_vault_recall_channel"
    private const val SIX_HOURS_MILLIS = 6L * 60L * 60L * 1000L

    private val _state = MutableStateFlow(ReminderSettingsState())
    val state: StateFlow<ReminderSettingsState> = _state.asStateFlow()

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val loaded = loadStateFromPrefs(appContext)
        _state.value = loaded
        syncAllAlarms(appContext, loaded.schedules)
        checkElapsedRecallWindow(appContext)
    }

    fun checkElapsedRecallWindow(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_MILLIS, 0L)
        if (lastPrompt == 0L) {
            prefs.edit().putLong(KEY_LAST_PROMPT_MILLIS, now).apply()
            return
        }
        val activeSchedules = _state.value.schedules.filter { it.isEnabled }
        val minIntervalMillis = activeSchedules
            .filter { it.mode == "INTERVAL" }
            .minOfOrNull { it.intervalMillis() }
            ?: SIX_HOURS_MILLIS

        if (activeSchedules.isNotEmpty() && now - lastPrompt >= minIntervalMillis) {
            val label = activeSchedules.first().label
            prefs.edit()
                .putLong(KEY_LAST_PROMPT_MILLIS, now)
                .putBoolean(KEY_IN_APP_DUE, true)
                .putString(KEY_ACTIVE_RECALL_LABEL, label)
                .apply()
            _state.value = _state.value.copy(
                isInAppRecallDue = true,
                activeRecallLabel = label
            )
        }
    }

    fun dismissInAppRecallPrompt(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IN_APP_DUE, false)
            .putLong(KEY_LAST_PROMPT_MILLIS, System.currentTimeMillis())
            .apply()
        _state.value = _state.value.copy(isInAppRecallDue = false)
    }

    fun addCustomSchedule(
        context: Context,
        label: String,
        mode: String,
        intervalHours: Int = 6,
        intervalMinutes: Int = 0,
        hourOfDay: Int = 20,
        minuteOfDay: Int = 0
    ) {
        val appContext = context.applicationContext
        val cleanLabel = label.trim().ifBlank {
            if (mode == "DAILY_TIME") "Custom Daily Recall" else "Custom Interval Recall"
        }
        val newSchedule = RecallReminderSchedule(
            id = UUID.randomUUID().toString(),
            label = cleanLabel,
            mode = if (mode == "DAILY_TIME") "DAILY_TIME" else "INTERVAL",
            intervalHours = intervalHours.coerceIn(0, 72),
            intervalMinutes = intervalMinutes.coerceIn(0, 59),
            hourOfDay = hourOfDay.coerceIn(0, 23),
            minuteOfDay = minuteOfDay.coerceIn(0, 59),
            isEnabled = true
        )
        val updated = _state.value.schedules + newSchedule
        saveSchedulesAndSync(appContext, updated)
    }

    fun toggleSchedule(context: Context, scheduleId: String, enabled: Boolean) {
        val appContext = context.applicationContext
        val updated = _state.value.schedules.map {
            if (it.id == scheduleId) it.copy(isEnabled = enabled) else it
        }
        saveSchedulesAndSync(appContext, updated)
    }

    fun deleteSchedule(context: Context, scheduleId: String) {
        val appContext = context.applicationContext
        cancelAlarmForSchedule(appContext, scheduleId)
        val updated = _state.value.schedules.filterNot { it.id == scheduleId }
        saveSchedulesAndSync(appContext, updated)
    }

    fun updateQuietHours(
        context: Context,
        enabled: Boolean,
        startHour: Int = _state.value.quietStartHour,
        endHour: Int = _state.value.quietEndHour
    ) {
        val appContext = context.applicationContext
        val sHour = startHour.coerceIn(0, 23)
        val eHour = endHour.coerceIn(0, 23)
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_QUIET_ENABLED, enabled)
            .putInt(KEY_QUIET_START, sHour)
            .putInt(KEY_QUIET_END, eHour)
            .apply()
        _state.value = _state.value.copy(
            quietHoursEnabled = enabled,
            quietStartHour = sHour,
            quietEndHour = eHour
        )
    }

    fun triggerImmediateTestNotification(context: Context, label: String = "Transaction Recall Reminder") {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IN_APP_DUE, true)
            .putString(KEY_ACTIVE_RECALL_LABEL, label)
            .putLong(KEY_LAST_PROMPT_MILLIS, System.currentTimeMillis())
            .apply()
        _state.value = _state.value.copy(
            isInAppRecallDue = true,
            activeRecallLabel = label
        )
        postSystemRecallNotification(appContext, label, ignoreQuietHours = true)
    }

    fun onAlarmFired(context: Context, scheduleId: String?, scheduleLabel: String?) {
        val appContext = context.applicationContext
        val current = loadStateFromPrefs(appContext)
        _state.value = current
        val matched = current.schedules.firstOrNull { it.id == scheduleId }
        val effectiveLabel = scheduleLabel?.takeIf { it.isNotBlank() }
            ?: matched?.label
            ?: "6-Hour Transaction Recall"

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IN_APP_DUE, true)
            .putString(KEY_ACTIVE_RECALL_LABEL, effectiveLabel)
            .putLong(KEY_LAST_PROMPT_MILLIS, System.currentTimeMillis())
            .apply()

        _state.value = current.copy(
            isInAppRecallDue = true,
            activeRecallLabel = effectiveLabel
        )

        postSystemRecallNotification(appContext, effectiveLabel, ignoreQuietHours = false)

        if (matched != null && matched.isEnabled) {
            scheduleSingleAlarm(appContext, matched)
        }
    }

    private fun isWithinQuietHours(state: ReminderSettingsState): Boolean {
        if (!state.quietHoursEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = state.quietStartHour
        val end = state.quietEndHour
        return if (start <= end) {
            hour in start until end
        } else {
            hour >= start || hour < end
        }
    }

    fun postSystemRecallNotification(
        context: Context,
        scheduleLabel: String,
        ignoreQuietHours: Boolean = false
    ) {
        val appContext = context.applicationContext
        val currentState = _state.value
        if (!ignoreQuietHours && isWithinQuietHours(currentState)) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureNotificationChannel(appContext)

        val voiceIntent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("vault://voice-record")
            putExtra(EXTRA_START_VOICE_RECORD, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val voicePendingIntent = PendingIntent.getActivity(
            appContext,
            1001,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val manualIntent = Intent(appContext, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_RECORD_SHEET, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val manualPendingIntent = PendingIntent.getActivity(
            appContext,
            1002,
            manualIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Recall & Log Your Transactions")
            .setContentText("$scheduleLabel: Tap to speak your recent transactions hands-free in English.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$scheduleLabel: Have you made any purchases, payments, or transfers recently? Tap to speak in English and Finance Vault will record them automatically."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(voicePendingIntent)
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                "Speak Transaction",
                voicePendingIntent
            )
            .addAction(
                android.R.drawable.ic_input_add,
                "Record Manually",
                manualPendingIntent
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify((scheduleLabel.hashCode() and 0x7FFFFFFF).coerceAtLeast(2000), notification)
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction Recall Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you every 6 hours and at custom time periods to recall and voice-record transactions."
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun saveSchedulesAndSync(context: Context, schedules: List<RecallReminderSchedule>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        schedules.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("label", s.label)
                put("mode", s.mode)
                put("intervalHours", s.intervalHours)
                put("intervalMinutes", s.intervalMinutes)
                put("hourOfDay", s.hourOfDay)
                put("minuteOfDay", s.minuteOfDay)
                put("isEnabled", s.isEnabled)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SCHEDULES_JSON, arr.toString()).apply()
        _state.value = _state.value.copy(schedules = schedules)
        syncAllAlarms(context, schedules)
    }

    fun syncAllAlarms(context: Context, schedules: List<RecallReminderSchedule>) {
        schedules.forEach { schedule ->
            if (schedule.isEnabled) {
                scheduleSingleAlarm(context, schedule)
            } else {
                cancelAlarmForSchedule(context, schedule.id)
            }
        }
    }

    private fun scheduleSingleAlarm(context: Context, schedule: RecallReminderSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TransactionRecallReminderReceiver::class.java).apply {
            action = ACTION_TRANSACTION_RECALL_REMINDER
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_SCHEDULE_LABEL, schedule.label)
        }
        val requestCode = (schedule.id.hashCode() and 0x7FFFFFFF).coerceAtLeast(3000)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = if (schedule.mode == "DAILY_TIME") {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, schedule.hourOfDay.coerceIn(0, 23))
                set(Calendar.MINUTE, schedule.minuteOfDay.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis() + 5_000L) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            cal.timeInMillis
        } else {
            System.currentTimeMillis() + schedule.intervalMillis()
        }

        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarmForSchedule(context: Context, scheduleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TransactionRecallReminderReceiver::class.java).apply {
            action = ACTION_TRANSACTION_RECALL_REMINDER
        }
        val requestCode = (scheduleId.hashCode() and 0x7FFFFFFF).coerceAtLeast(3000)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            runCatching { alarmManager.cancel(pendingIntent) }
        }
    }

    private fun loadStateFromPrefs(context: Context): ReminderSettingsState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_SCHEDULES_JSON, null)
        val schedules = mutableListOf<RecallReminderSchedule>()
        if (!rawJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    schedules.add(
                        RecallReminderSchedule(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            label = obj.optString("label", "Transaction Recall"),
                            mode = obj.optString("mode", "INTERVAL"),
                            intervalHours = obj.optInt("intervalHours", 6),
                            intervalMinutes = obj.optInt("intervalMinutes", 0),
                            hourOfDay = obj.optInt("hourOfDay", 20),
                            minuteOfDay = obj.optInt("minuteOfDay", 0),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
            }
        }
        if (schedules.isEmpty()) {
            // Default 6-Hour Transaction Recall Schedule + Evening Daily Slot
            schedules.add(
                RecallReminderSchedule(
                    id = "default_every_6_hours",
                    label = "Every 6 Hours Transaction Recall",
                    mode = "INTERVAL",
                    intervalHours = 6,
                    intervalMinutes = 0,
                    isEnabled = true
                )
            )
        }
        return ReminderSettingsState(
            schedules = schedules,
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_ENABLED, true),
            quietStartHour = prefs.getInt(KEY_QUIET_START, 22),
            quietEndHour = prefs.getInt(KEY_QUIET_END, 7),
            isInAppRecallDue = prefs.getBoolean(KEY_IN_APP_DUE, false),
            activeRecallLabel = prefs.getString(KEY_ACTIVE_RECALL_LABEL, "Every 6 Hours Transaction Recall")
                ?: "Every 6 Hours Transaction Recall"
        )
    }
}
