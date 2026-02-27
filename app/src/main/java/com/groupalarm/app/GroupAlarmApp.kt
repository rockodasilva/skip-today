package com.groupalarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GroupAlarmApp : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Alarmas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de alarmas"
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null) // Sound is handled by AlarmService
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
