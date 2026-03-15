package com.rsps1008.qrcode

import android.content.Context
import androidx.room.Room
import com.rsps1008.qrcode.ui.database.AppDatabase
import com.rsps1008.qrcode.ui.database.ScanResultDao

object Utils {
    private val DB_NAME: String = "qrcode1922.db"

    fun getDatabaseDao(applicationContext: Context): ScanResultDao {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DB_NAME).build()
        return db.resultDao()
    }
}