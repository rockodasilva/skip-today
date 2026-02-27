package com.groupalarm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groupalarm.app.alarm.AlarmScheduler
import com.groupalarm.app.data.model.Alarm
import com.groupalarm.app.data.model.AlarmGroup
import com.groupalarm.app.data.model.AlarmWithGroup
import com.groupalarm.app.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupWithAlarms(
    val group: AlarmGroup,
    val alarms: List<Alarm>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _showGroupDialog = MutableStateFlow(false)
    val showGroupDialog: StateFlow<Boolean> = _showGroupDialog

    private val _editingGroup = MutableStateFlow<AlarmGroup?>(null)
    val editingGroup: StateFlow<AlarmGroup?> = _editingGroup

    val groupsWithAlarms: StateFlow<List<GroupWithAlarms>> = combine(
        repository.getAllGroups(),
        repository.getAllAlarmsWithGroup()
    ) { groups, alarmsWithGroup ->
        groups.map { group ->
            GroupWithAlarms(
                group = group,
                alarms = alarmsWithGroup
                    .filter { it.alarm.groupId == group.id }
                    .map { it.alarm }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newEnabled = !alarm.isEnabled
            repository.setAlarmEnabled(alarm.id, newEnabled)
            if (newEnabled) {
                alarmScheduler.schedule(alarm.copy(isEnabled = true))
            } else {
                alarmScheduler.cancel(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmScheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
        }
    }

    fun showCreateGroupDialog() {
        _editingGroup.value = null
        _showGroupDialog.value = true
    }

    fun showEditGroupDialog(group: AlarmGroup) {
        _editingGroup.value = group
        _showGroupDialog.value = true
    }

    fun dismissGroupDialog() {
        _showGroupDialog.value = false
        _editingGroup.value = null
    }

    fun saveGroup(name: String) {
        viewModelScope.launch {
            val editing = _editingGroup.value
            if (editing != null) {
                repository.updateGroup(editing.copy(name = name))
            } else {
                repository.insertGroup(AlarmGroup(name = name))
            }
            dismissGroupDialog()
        }
    }

    fun toggleGroupSilence(group: AlarmGroup) {
        viewModelScope.launch {
            if (group.isSilencedToday()) {
                // Unsilence: clear the silenced date
                repository.updateGroup(group.copy(silencedDate = null))
            } else {
                // Silence for today
                repository.silenceGroupForToday(group.id)
            }
        }
    }

    fun deleteGroup(group: AlarmGroup) {
        viewModelScope.launch {
            // Cancel all alarms in this group first
            val allAlarms = groupsWithAlarms.value
                .find { it.group.id == group.id }?.alarms ?: emptyList()
            allAlarms.forEach { alarmScheduler.cancel(it) }
            repository.deleteGroup(group)
        }
    }

    fun ensureDefaultGroup() {
        viewModelScope.launch {
            // Query DB directly instead of using the StateFlow value,
            // which starts as emptyList() before the DB loads
            val dbGroups = repository.getGroupCount()
            if (dbGroups == 0) {
                repository.insertGroup(AlarmGroup(name = "General"))
            }
        }
    }
}
