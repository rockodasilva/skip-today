package com.groupalarm.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.groupalarm.app.data.model.Alarm
import com.groupalarm.app.data.model.AlarmWithGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Transaction
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsWithGroup(): Flow<List<AlarmWithGroup>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?

    @Transaction
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmWithGroupById(id: Long): AlarmWithGroup?

    @Query("SELECT * FROM alarms WHERE is_enabled = 1")
    suspend fun getEnabledAlarms(): List<Alarm>

    @Insert
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("UPDATE alarms SET is_enabled = :enabled WHERE id = :alarmId")
    suspend fun setEnabled(alarmId: Long, enabled: Boolean)
}
