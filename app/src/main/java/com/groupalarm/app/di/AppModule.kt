package com.groupalarm.app.di

import android.content.Context
import com.groupalarm.app.data.dao.AlarmDao
import com.groupalarm.app.data.dao.AlarmGroupDao
import com.groupalarm.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideAlarmGroupDao(db: AppDatabase): AlarmGroupDao = db.alarmGroupDao()
}
