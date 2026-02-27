package com.groupalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        Log.d("AlarmReceiver", "Alarm received: id=$alarmId, isSnooze=$isSnooze")

        // Start the service immediately — don't do async DB work in the receiver.
        // The service handles checking if the group is silenced, etc.
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        context.startForegroundService(serviceIntent)
    }
}
