package com.rsps1008.qrcode.ui.database

import androidx.room.*

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scanresult ORDER BY id DESC")
    suspend fun getAll(): List<ScanResult>

    @Insert
    suspend fun insert(result: ScanResult): Long

    @Insert
    suspend fun insertAll(results: List<ScanResult>)

    @Query("DELETE FROM scanresult")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(results: List<ScanResult>) {
        deleteAll()
        // Backups are exported newest-first. Insert oldest-first so the generated
        // IDs preserve the same history order after a restore.
        insertAll(results.asReversed())
    }

    @Query("UPDATE scanresult SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String?)

    @Query("UPDATE scanresult SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)


    @Delete
    suspend fun delete(result: ScanResult)
}
