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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.audiorecorder01.data.local.RecordingDao

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `recordings` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'COMPLETED'")
    }
}

@Database(entities = [RecordingEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
