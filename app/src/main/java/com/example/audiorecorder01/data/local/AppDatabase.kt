@file:Suppress(
    "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "RedundantSuppression",
    "RedundantSuppression", "RedundantSuppression"
)

package com.saurabh.sonicaudiorecorder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.audiorecorder01.data.local.RecordingDao

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
