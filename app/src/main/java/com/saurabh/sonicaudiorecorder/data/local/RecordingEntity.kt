package com.saurabh.sonicaudiorecorder.data.local

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val filePath: String,
    val durationMs: Long,
    val timestamp: Long,
    val fileSize: Long,
    val status: String = "COMPLETED" // COMPLETED or PENDING
)
