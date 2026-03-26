@file:Suppress("PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch", "PackageDirectoryMismatch",
    "PackageDirectoryMismatch", "PackageDirectoryMismatch"
)

package com.saurabh.sonicaudiorecorder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
