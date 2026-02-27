package com.groupalarm.app.ui.edit

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.groupalarm.app.data.model.Alarm

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(
    onBack: () -> Unit,
    viewModel: EditAlarmViewModel = hiltViewModel()
) {
    val hour by viewModel.hour.collectAsStateWithLifecycle()
    val minute by viewModel.minute.collectAsStateWithLifecycle()
    val daysOfWeek by viewModel.daysOfWeek.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val soundUri by viewModel.soundUri.collectAsStateWithLifecycle()
    val label by viewModel.label.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    // Set default group when groups load
    LaunchedEffect(groups, selectedGroupId) {
        if (selectedGroupId == null && groups.isNotEmpty()) {
            viewModel.selectGroup(groups.first().id)
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    // When the ViewModel loads alarm data from DB, update the TimePicker
    LaunchedEffect(hour, minute) {
        if (timePickerState.hour != hour) timePickerState.hour = hour
        if (timePickerState.minute != minute) timePickerState.minute = minute
    }

    // Sync time picker state changes back to ViewModel
    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        viewModel.setHour(timePickerState.hour)
        viewModel.setMinute(timePickerState.minute)
    }

    // Ringtone picker
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setSoundUri(uri?.toString() ?: "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Editar alarma" else "Nueva alarma") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(Icons.Default.Delete, "Eliminar")
                        }
                    }
                    TextButton(onClick = { viewModel.save() }) {
                        Text("Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time picker
            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(24.dp))

            // Days of week
            Text(
                "Repetir",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Alarm.DAY_VALUES.zip(Alarm.DAY_LABELS).forEach { (value, dayLabel) ->
                    FilterChip(
                        selected = daysOfWeek and value != 0,
                        onClick = { viewModel.toggleDay(value) },
                        label = { Text(dayLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group selector
            Text(
                "Grupo",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                groups.forEach { group ->
                    FilterChip(
                        selected = selectedGroupId == group.id,
                        onClick = { viewModel.selectGroup(group.id) },
                        label = { Text(group.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Label
            OutlinedTextField(
                value = label,
                onValueChange = { viewModel.setLabel(it) },
                label = { Text("Etiqueta (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sound selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sonido",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (soundUri.isNotEmpty()) {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    Uri.parse(soundUri)
                                )
                            }
                        }
                        ringtoneLauncher.launch(intent)
                    }
                ) {
                    Icon(Icons.Default.MusicNote, null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = if (soundUri.isNotEmpty()) {
                            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(soundUri))
                            ringtone?.getTitle(context) ?: "Seleccionar"
                        } else "Seleccionar"
                    )
                }
            }
        }
    }
}
