package com.groupalarm.app.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groupalarm.app.data.db.AppDatabase
import com.groupalarm.app.ui.theme.GroupAlarmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    companion object {
        const val ACTION_FINISH = "com.groupalarm.ACTION_FINISH_ALARM_ACTIVITY"
    }

    private var alarmLabel by mutableStateOf("")
    private var groupName by mutableStateOf("")
    private var currentTime by mutableStateOf("")
    private var alarmId: Long = -1

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAndCleanup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupLockScreen()
        super.onCreate(savedInstanceState)

        registerReceiver(finishReceiver, IntentFilter(ACTION_FINISH), RECEIVER_NOT_EXPORTED)
        handleIntent(intent)

        setContent {
            GroupAlarmTheme {
                AlarmScreen(
                    time = currentTime,
                    label = alarmLabel,
                    groupName = groupName,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm() },
                    onSilenceGroup = { silenceGroup() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A new alarm came in while this activity is already showing.
        // Update with the new alarm's data.
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        alarmId = intent.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1)
        currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@AlarmActivity)
            val alarmWithGroup = db.alarmDao().getAlarmWithGroupById(alarmId)
            alarmLabel = alarmWithGroup?.alarm?.label ?: ""
            groupName = alarmWithGroup?.group?.name ?: ""
        }
    }

    private fun setupLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dismissAlarm() {
        stopAndFinish()
    }

    private fun snoozeAlarm() {
        val scheduler = AlarmScheduler(this)
        scheduler.scheduleSnooze(alarmId)
        stopAndFinish()
    }

    private fun silenceGroup() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@AlarmActivity)
            val alarm = db.alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                db.alarmGroupDao().silenceGroupForDate(
                    alarm.groupId,
                    LocalDate.now().toString()
                )
            }
        }
        stopAndFinish()
    }

    private fun stopAndFinish() {
        AlarmService.stopAlarm(this)
        // Also dismiss the notification explicitly
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(AlarmService.NOTIFICATION_ID)
        finishAndCleanup()
    }

    private fun finishAndCleanup() {
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(finishReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
        super.onDestroy()
    }
}

@Composable
private fun AlarmScreen(
    time: String,
    label: String,
    groupName: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onSilenceGroup: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = time,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (groupName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grupo: $groupName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Apagar", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Snooze (5 min)", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSilenceGroup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    "Basta por hoy",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (groupName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Silencia el grupo \"$groupName\" por hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
