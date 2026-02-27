package com.groupalarm.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class AlarmWithGroup(
    @Embedded val alarm: Alarm,
    @Relation(
        parentColumn = "group_id",
        entityColumn = "id"
    )
    val group: AlarmGroup
)
