package com.example.domain.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TransactionRecallReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            TransactionRecallReminderManager.ACTION_TRANSACTION_RECALL_REMINDER -> {
                val scheduleId = intent.getStringExtra(TransactionRecallReminderManager.EXTRA_SCHEDULE_ID)
                val scheduleLabel = intent.getStringExtra(TransactionRecallReminderManager.EXTRA_SCHEDULE_LABEL)
                TransactionRecallReminderManager.onAlarmFired(context, scheduleId, scheduleLabel)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                TransactionRecallReminderManager.initialize(context)
            }
        }
    }
}
