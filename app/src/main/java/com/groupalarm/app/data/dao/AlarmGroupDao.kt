package com.groupalarm.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.groupalarm.app.data.model.AlarmGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmGroupDao {

    @Query("SELECT * FROM alarm_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<AlarmGroup>>

    @Query("SELECT * FROM alarm_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): AlarmGroup?

    @Insert
    suspend fun insert(group: AlarmGroup): Long

    @Update
    suspend fun update(group: AlarmGroup)

    @Delete
    suspend fun delete(group: AlarmGroup)

    @Query("UPDATE alarm_groups SET silenced_date = :date WHERE id = :groupId")
    suspend fun silenceGroupForDate(groupId: Long, date: String)

    @Query("SELECT silenced_date FROM alarm_groups WHERE id = :groupId")
    suspend fun getSilencedDate(groupId: Long): String?

    @Query("SELECT COUNT(*) FROM alarm_groups")
    suspend fun getCount(): Int
}
