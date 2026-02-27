package com.groupalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.groupalarm.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val scheduler = AlarmScheduler(context)
                val enabledAlarms = db.alarmDao().getEnabledAlarms()
                enabledAlarms.forEach { alarm ->
                    scheduler.schedule(alarm)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
