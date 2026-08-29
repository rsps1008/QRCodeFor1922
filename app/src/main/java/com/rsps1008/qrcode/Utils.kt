package com.rsps1008.qrcode

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rsps1008.qrcode.ui.database.AppDatabase
import com.rsps1008.qrcode.ui.database.ScanResultDao

object Utils {
    private val DB_NAME: String = "qrcode1922.db"

    fun getDatabaseDao(applicationContext: Context): ScanResultDao {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        return db.resultDao()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE scanresult ADD COLUMN title TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE scanresult ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        }
    }
}
