package com.groupalarm.app.data.repository

import com.groupalarm.app.data.dao.AlarmDao
import com.groupalarm.app.data.dao.AlarmGroupDao
import com.groupalarm.app.data.model.Alarm
import com.groupalarm.app.data.model.AlarmGroup
import com.groupalarm.app.data.model.AlarmWithGroup
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    private val groupDao: AlarmGroupDao
) {
    fun getAllAlarmsWithGroup(): Flow<List<AlarmWithGroup>> = alarmDao.getAllAlarmsWithGroup()

    fun getAllGroups(): Flow<List<AlarmGroup>> = groupDao.getAllGroups()

    suspend fun getAlarmById(id: Long): Alarm? = alarmDao.getAlarmById(id)

    suspend fun getAlarmWithGroupById(id: Long): AlarmWithGroup? = alarmDao.getAlarmWithGroupById(id)

    suspend fun getEnabledAlarms(): List<Alarm> = alarmDao.getEnabledAlarms()

    suspend fun insertAlarm(alarm: Alarm): Long = alarmDao.insert(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.update(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.delete(alarm)

    suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean) = alarmDao.setEnabled(alarmId, enabled)

    suspend fun getGroupById(id: Long): AlarmGroup? = groupDao.getGroupById(id)

    suspend fun insertGroup(group: AlarmGroup): Long = groupDao.insert(group)

    suspend fun updateGroup(group: AlarmGroup) = groupDao.update(group)

    suspend fun deleteGroup(group: AlarmGroup) = groupDao.delete(group)

    suspend fun silenceGroupForToday(groupId: Long) {
        groupDao.silenceGroupForDate(groupId, LocalDate.now().toString())
    }

    suspend fun isGroupSilencedToday(groupId: Long): Boolean {
        val silencedDate = groupDao.getSilencedDate(groupId)
        return silencedDate == LocalDate.now().toString()
    }

    suspend fun getGroupCount(): Int = groupDao.getCount()
}
