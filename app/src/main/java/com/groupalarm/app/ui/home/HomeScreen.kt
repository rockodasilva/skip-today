package com.groupalarm.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.groupalarm.app.data.model.Alarm
import com.groupalarm.app.data.model.AlarmGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val groupsWithAlarms by viewModel.groupsWithAlarms.collectAsStateWithLifecycle()
    val showGroupDialog by viewModel.showGroupDialog.collectAsStateWithLifecycle()
    val editingGroup by viewModel.editingGroup.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.ensureDefaultGroup()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skip Today") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.showCreateGroupDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Crear grupo")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Default.Add, contentDescription = "Agregar alarma")
            }
        }
    ) { padding ->
        if (groupsWithAlarms.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No hay alarmas configuradas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tocá + para agregar una alarma",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val expandedGroups = remember { mutableStateMapOf<Long, Boolean>() }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(groupsWithAlarms, key = { it.group.id }) { groupWithAlarms ->
                    val isExpanded = expandedGroups.getOrPut(groupWithAlarms.group.id) { true }
                    GroupCard(
                        groupWithAlarms = groupWithAlarms,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedGroups[groupWithAlarms.group.id] = !isExpanded
                        },
                        onEditGroup = { viewModel.showEditGroupDialog(groupWithAlarms.group) },
                        onDeleteGroup = { viewModel.deleteGroup(groupWithAlarms.group) },
                        onToggleSilence = { viewModel.toggleGroupSilence(groupWithAlarms.group) },
                        onToggleAlarm = { viewModel.toggleAlarm(it) },
                        onEditAlarm = onEditAlarm,
                        onDeleteAlarm = { viewModel.deleteAlarm(it) }
                    )
                }
            }
        }

        if (showGroupDialog) {
            GroupNameDialog(
                initialName = editingGroup?.name ?: "",
                isEditing = editingGroup != null,
                onDismiss = { viewModel.dismissGroupDialog() },
                onConfirm = { viewModel.saveGroup(it) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    groupWithAlarms: GroupWithAlarms,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onToggleSilence: () -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onEditAlarm: (Long) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit
) {
    val group = groupWithAlarms.group
    val isSilenced = group.isSilencedToday()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = if (isSilenced) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column {
            // Group header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onToggleExpand)
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group name + alarm count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append("${groupWithAlarms.alarms.size} alarma")
                            if (groupWithAlarms.alarms.size != 1) append("s")
                            if (isSilenced) append(" · Silenciado hoy")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSilenced) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Silence toggle
                IconButton(onClick = onToggleSilence) {
                    Icon(
                        if (isSilenced) Icons.Default.NotificationsOff
                        else Icons.Filled.NotificationsActive,
                        contentDescription = if (isSilenced) "Reactivar grupo" else "Silenciar grupo hoy",
                        tint = if (isSilenced) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Edit button
                IconButton(onClick = onEditGroup) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar grupo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete button
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar grupo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expand/collapse
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir"
                )
            }

            // Alarms list
            if (isExpanded) {
                groupWithAlarms.alarms.forEach { alarm ->
                    AlarmItem(
                        alarm = alarm,
                        isSilenced = isSilenced,
                        onToggle = { onToggleAlarm(alarm) },
                        onClick = { onEditAlarm(alarm.id) },
                        onDelete = { onDeleteAlarm(alarm) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar grupo") },
            text = { Text("Se eliminarán todas las alarmas del grupo \"${group.name}\".") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteGroup()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmItem(
    alarm: Alarm,
    isSilenced: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alarm.timeFormatted(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = if (alarm.isEnabled && !isSilenced) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
            )
            if (alarm.label.isNotEmpty()) {
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = daysText(alarm.daysOfWeek),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = { onToggle() }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Eliminar") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }
    }
}

private fun daysText(daysOfWeek: Int): String {
    if (daysOfWeek == 0) return "Una vez"
    if (daysOfWeek == Alarm.ALL_DAYS) return "Todos los días"
    val weekdays = Alarm.MONDAY or Alarm.TUESDAY or Alarm.WEDNESDAY or Alarm.THURSDAY or Alarm.FRIDAY
    if (daysOfWeek == weekdays) return "Lun a Vie"
    val weekend = Alarm.SATURDAY or Alarm.SUNDAY
    if (daysOfWeek == weekend) return "Sáb y Dom"

    return Alarm.DAY_VALUES.zip(Alarm.DAY_LABELS)
        .filter { (value, _) -> daysOfWeek and value != 0 }
        .joinToString(", ") { (_, label) -> label }
}

@Composable
private fun GroupNameDialog(
    initialName: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar grupo" else "Nuevo grupo") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del grupo") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
