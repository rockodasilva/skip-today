package com.groupalarm.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.groupalarm.app.GroupAlarmApp
import com.groupalarm.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val ACTION_DISMISS = "com.groupalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.groupalarm.ACTION_SNOOZE"
        const val ACTION_SILENCE_GROUP = "com.groupalarm.ACTION_SILENCE_GROUP"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "AlarmService"

        fun stopAlarm(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1) ?: -1
        val isSnooze = intent?.getBooleanExtra(AlarmReceiver.EXTRA_IS_SNOOZE, false) ?: false

        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(TAG, "Service started for alarm $alarmId, isSnooze=$isSnooze")

        // CRITICAL: Call startForeground() immediately with the correct service type.
        // On Android 14+ (API 34), omitting the type causes MissingForegroundServiceTypeException.
        val placeholderNotification = buildNotification(alarmId, "Alarma", "")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    placeholderNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, placeholderNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start vibration immediately
        startVibration()

        // Load alarm details from DB, check if silenced, then ring
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(this@AlarmService)
                val alarmWithGroup = db.alarmDao().getAlarmWithGroupById(alarmId)
                val alarm = alarmWithGroup?.alarm
                val group = alarmWithGroup?.group

                if (alarm == null || (!alarm.isEnabled && !isSnooze)) {
                    Log.d(TAG, "Alarm not found or disabled, stopping")
                    stopSelf()
                    return@launch
                }

                // Check if the group is silenced for today
                if (!isSnooze && group != null && group.silencedDate == LocalDate.now().toString()) {
                    Log.d(TAG, "Group '${group.name}' is silenced today, skipping alarm")
                    if (alarm.daysOfWeek != 0) {
                        val scheduler = AlarmScheduler(this@AlarmService)
                        scheduler.schedule(alarm)
                    }
                    stopSelf()
                    return@launch
                }

                // Update notification with real alarm details
                val notification = buildNotification(alarmId, alarm.label.ifEmpty { "Alarma" }, group?.name ?: "")
                val notificationManager = getSystemService(android.app.NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)

                // Play sound
                val soundUri = if (alarm.soundUri.isNotEmpty()) Uri.parse(alarm.soundUri) else null
                playSound(soundUri)

                // Reschedule repeating alarm for next occurrence
                if (alarm.daysOfWeek != 0 && !isSnooze) {
                    val scheduler = AlarmScheduler(this@AlarmService)
                    scheduler.schedule(alarm)
                }

                // Launch full-screen activity
                val activityIntent = Intent(this@AlarmService, AlarmActivity::class.java).apply {
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(activityIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error in alarm coroutine", e)
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(alarmId: Long, label: String, groupName: String): Notification {
        val dismissIntent = createActionIntent(ACTION_DISMISS, alarmId)
        val snoozeIntent = createActionIntent(ACTION_SNOOZE, alarmId)
        val silenceGroupIntent = createActionIntent(ACTION_SILENCE_GROUP, alarmId)

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, alarmId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (label.isNotEmpty()) label else "Alarma"
        val text = if (groupName.isNotEmpty()) "Grupo: $groupName" else ""

        return NotificationCompat.Builder(this, GroupAlarmApp.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Apagar", dismissIntent)
            .addAction(android.R.drawable.ic_media_pause, "Snooze 5m", snoozeIntent)
            .addAction(android.R.drawable.ic_menu_today, "Basta por hoy", silenceGroupIntent)
            .build()
    }

    private fun createActionIntent(action: String, alarmId: Long): PendingIntent {
        val intent = Intent(this, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            this, "$action$alarmId".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun playSound(soundUri: Uri?) {
        // Try MediaPlayer first, fall back to Ringtone API
        try {
            val uri = soundUri
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            if (uri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                Log.d(TAG, "Playing alarm sound via MediaPlayer: $uri")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer failed, trying Ringtone API", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }

        // Fallback: use Ringtone API which is more tolerant
        try {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (fallbackUri != null) {
                ringtone = RingtoneManager.getRingtone(this, fallbackUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
                Log.d(TAG, "Playing alarm sound via Ringtone API: $fallbackUri")
            } else {
                Log.w(TAG, "No alarm sound URI available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ringtone API also failed", e)
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }

            val pattern = longArrayOf(0, 500, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }
}
