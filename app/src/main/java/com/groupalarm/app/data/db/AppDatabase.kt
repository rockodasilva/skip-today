package com.groupalarm.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.groupalarm.app.data.dao.AlarmDao
import com.groupalarm.app.data.dao.AlarmGroupDao
import com.groupalarm.app.data.model.Alarm
import com.groupalarm.app.data.model.AlarmGroup

@Database(
    entities = [Alarm::class, AlarmGroup::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmGroupDao(): AlarmGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "groupalarm.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun setInstance(db: AppDatabase) {
            INSTANCE = db
        }
    }
}
