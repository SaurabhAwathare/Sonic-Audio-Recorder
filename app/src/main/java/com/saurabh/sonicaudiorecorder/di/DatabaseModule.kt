package com.saurabh.sonicaudiorecorder.di

import android.content.Context
import androidx.room.Room
import com.saurabh.sonicaudiorecorder.data.local.AppDatabase
import com.saurabh.sonicaudiorecorder.data.local.MIGRATION_1_2
import com.example.audiorecorder01.data.local.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "recordings_db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideRecordingDao(database: AppDatabase): RecordingDao {
        return database.recordingDao()
    }
}
