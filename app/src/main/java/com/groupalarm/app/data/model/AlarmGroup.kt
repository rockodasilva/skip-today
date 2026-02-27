package com.groupalarm.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "alarm_groups")
data class AlarmGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "silenced_date")
    val silencedDate: String? = null
) {
    fun isSilencedToday(): Boolean {
        return silencedDate == LocalDate.now().toString()
    }
}
