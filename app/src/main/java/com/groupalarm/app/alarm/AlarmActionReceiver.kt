package com.groupalarm.app.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.groupalarm.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        // Stop the alarm service and dismiss notification
        AlarmService.stopAlarm(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(AlarmService.NOTIFICATION_ID)

        // Close the AlarmActivity if it's open
        context.sendBroadcast(Intent(AlarmActivity.ACTION_FINISH))

        when (intent.action) {
            AlarmService.ACTION_SNOOZE -> {
                val scheduler = AlarmScheduler(context)
                scheduler.scheduleSnooze(alarmId)
            }

            AlarmService.ACTION_SILENCE_GROUP -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null) {
                            db.alarmGroupDao().silenceGroupForDate(
                                alarm.groupId,
                                LocalDate.now().toString()
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
