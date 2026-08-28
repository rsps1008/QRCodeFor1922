package com.rsps1008.qrcode.ui.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    version = 4,
    entities = [ScanResult::class],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resultDao(): ScanResultDao
}
