package com.rsps1008.qrcode.ui.database

import androidx.room.*

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scanresult ORDER BY id DESC")
    suspend fun getAll(): List<ScanResult>

    @Insert
    suspend fun insert(result: ScanResult): Long

    @Query("UPDATE scanresult SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)


    @Delete
    suspend fun delete(result: ScanResult)
}
