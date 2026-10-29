package com.example.audiorecorder01.data.local

import androidx.room.*
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE status = 'COMPLETED' ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE status = 'PENDING'")
    suspend fun getPendingRecordings(): List<RecordingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)
}
