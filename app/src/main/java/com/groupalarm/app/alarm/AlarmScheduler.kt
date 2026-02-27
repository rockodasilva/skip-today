package com.groupalarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.groupalarm.app.data.model.Alarm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return

        try {
            // setAlarmClock() is exempt from SCHEDULE_EXACT_ALARM restriction
            val triggerTime = getNextTriggerTime(alarm)
            val pendingIntent = createPendingIntent(alarm)

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
            Log.d("AlarmScheduler", "Alarm ${alarm.id} scheduled for $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling alarm", e)
        }
    }

    fun scheduleSnooze(alarmId: Long) {
        try {
            val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt() + 100000, // Offset to avoid conflict with regular alarm
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(snoozeTime, pendingIntent),
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling snooze", e)
        }
    }

    fun cancel(alarm: Alarm) {
        val pendingIntent = createPendingIntent(alarm)
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun getNextTriggerTime(alarm: Alarm): Long {
            val now = LocalDateTime.now()
            val alarmTime = LocalTime.of(alarm.hour, alarm.minute)

            if (alarm.daysOfWeek == 0) {
                // One-time alarm: today if time hasn't passed, otherwise tomorrow
                val candidate = LocalDate.now().atTime(alarmTime)
                val trigger = if (candidate.isAfter(now)) candidate
                else candidate.plusDays(1)
                return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            // Repeating alarm: find the next matching day
            for (offset in 0..7) {
                val candidateDate = LocalDate.now().plusDays(offset.toLong())
                val candidateDateTime = candidateDate.atTime(alarmTime)

                // Skip if it's today and the time has passed
                if (offset == 0 && !candidateDateTime.isAfter(now)) continue

                val javaDayOfWeek = candidateDate.dayOfWeek
                val alarmDay = dayOfWeekToBitmask(javaDayOfWeek)

                if (alarm.daysOfWeek and alarmDay != 0) {
                    return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }

            // Fallback (should never reach here if at least one day is selected)
            val fallback = LocalDate.now().plusDays(1).atTime(alarmTime)
            return fallback.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        private fun dayOfWeekToBitmask(day: DayOfWeek): Int = when (day) {
            DayOfWeek.MONDAY -> Alarm.MONDAY
            DayOfWeek.TUESDAY -> Alarm.TUESDAY
            DayOfWeek.WEDNESDAY -> Alarm.WEDNESDAY
            DayOfWeek.THURSDAY -> Alarm.THURSDAY
            DayOfWeek.FRIDAY -> Alarm.FRIDAY
            DayOfWeek.SATURDAY -> Alarm.SATURDAY
            DayOfWeek.SUNDAY -> Alarm.SUNDAY
        }
    }
}
