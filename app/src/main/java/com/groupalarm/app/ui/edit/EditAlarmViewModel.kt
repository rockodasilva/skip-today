package com.groupalarm.app.ui.edit

import android.media.RingtoneManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groupalarm.app.alarm.AlarmScheduler
import com.groupalarm.app.data.model.Alarm
import com.groupalarm.app.data.model.AlarmGroup
import com.groupalarm.app.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditAlarmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>("alarmId") ?: -1L
    val isEditing: Boolean = alarmId > 0

    val hour = MutableStateFlow(7)
    val minute = MutableStateFlow(0)
    val daysOfWeek = MutableStateFlow(Alarm.ALL_DAYS)
    val selectedGroupId = MutableStateFlow<Long?>(null)
    val soundUri = MutableStateFlow("")
    val label = MutableStateFlow("")

    val groups: StateFlow<List<AlarmGroup>> = repository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        if (isEditing) {
            viewModelScope.launch {
                repository.getAlarmById(alarmId)?.let { alarm ->
                    hour.value = alarm.hour
                    minute.value = alarm.minute
                    daysOfWeek.value = alarm.daysOfWeek
                    selectedGroupId.value = alarm.groupId
                    soundUri.value = alarm.soundUri
                    label.value = alarm.label
                }
            }
        } else {
            // Default sound
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            soundUri.value = defaultUri?.toString() ?: ""
        }
    }

    fun setHour(h: Int) { hour.value = h }
    fun setMinute(m: Int) { minute.value = m }
    fun setLabel(l: String) { label.value = l }
    fun setSoundUri(uri: String) { soundUri.value = uri }

    fun toggleDay(day: Int) {
        daysOfWeek.value = daysOfWeek.value xor day
    }

    fun selectGroup(groupId: Long) {
        selectedGroupId.value = groupId
    }

    fun save() {
        viewModelScope.launch {
            val groupId = selectedGroupId.value ?: groups.value.firstOrNull()?.id ?: return@launch

            val alarm = Alarm(
                id = if (isEditing) alarmId else 0,
                groupId = groupId,
                hour = hour.value,
                minute = minute.value,
                daysOfWeek = daysOfWeek.value,
                isEnabled = true,
                soundUri = soundUri.value,
                label = label.value
            )

            if (isEditing) {
                repository.updateAlarm(alarm)
                alarmScheduler.cancel(alarm)
            } else {
                val id = repository.insertAlarm(alarm)
                alarmScheduler.schedule(alarm.copy(id = id))
                _saved.value = true
                return@launch
            }

            alarmScheduler.schedule(alarm)
            _saved.value = true
        }
    }

    fun delete() {
        if (!isEditing) return
        viewModelScope.launch {
            repository.getAlarmById(alarmId)?.let { alarm ->
                alarmScheduler.cancel(alarm)
                repository.deleteAlarm(alarm)
            }
            _saved.value = true
        }
    }
}
