package com.groupalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        private const val TAG = "AlarmReceiver"
        private var wakeLock: PowerManager.WakeLock? = null

        fun releaseWakeLock() {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Screen wake lock released")
                }
            }
            wakeLock = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        Log.d(TAG, "Alarm received: id=$alarmId, isSnooze=$isSnooze")

        // Acquire a wake lock to physically turn the screen on.
        // Without this, on real phones with locked screens, the full-screen
        // intent may not display and the screen stays off.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP
                or PowerManager.ON_AFTER_RELEASE,
            "groupalarm:screen_wake"
        ).apply {
            acquire(60_000L) // auto-release after 60 seconds max
        }
        Log.d(TAG, "Screen wake lock acquired")

        // Start the service immediately — don't do async DB work in the receiver.
        // The service handles checking if the group is silenced, etc.
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        context.startForegroundService(serviceIntent)
    }
}
