package com.rsps1008.qrcode.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsps1008.qrcode.Utils
import com.rsps1008.qrcode.ui.database.ScanResult
import kotlinx.coroutines.launch
import java.util.*

class ScanResultViewModel: ViewModel() {
    val _resultData: MutableLiveData<List<ScanResult>> = MutableLiveData()
    private var showFavoritesOnly = false
    val resultData: LiveData<List<ScanResult>>
        get() = _resultData

    fun getAllResult(applicationContext: Context) {
        viewModelScope.launch {
            val resultDao = Utils.getDatabaseDao(applicationContext)
            publishResults(resultDao.getAll())
        }
    }

    fun setShowFavoritesOnly(applicationContext: Context, showOnly: Boolean) {
        showFavoritesOnly = showOnly
        getAllResult(applicationContext)
    }

    fun deleteResult(applicationContext: Context, result: ScanResult) {
        viewModelScope.launch {
            Utils.getDatabaseDao(applicationContext).delete(result)
            getAllResult(applicationContext)
        }
    }

    fun toggleFavorite(applicationContext: Context, result: ScanResult) {
        viewModelScope.launch {
            Utils.getDatabaseDao(applicationContext).updateFavorite(result.id, !result.isFavorite)
            getAllResult(applicationContext)
        }
    }

    fun updateTitle(applicationContext: Context, result: ScanResult, title: String?) {
        viewModelScope.launch {
            Utils.getDatabaseDao(applicationContext).updateTitle(result.id.toLong(), title)
            getAllResult(applicationContext)
        }
    }

    private fun publishResults(results: List<ScanResult>) {
        _resultData.value = if (showFavoritesOnly) results.filter { it.isFavorite } else results
    }

}
