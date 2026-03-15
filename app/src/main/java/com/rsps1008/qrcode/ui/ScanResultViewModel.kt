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
    val resultData: LiveData<List<ScanResult>>
        get() = _resultData

    fun getAllResult(applicationContext: Context) {
        viewModelScope.launch {
            val resultDao = Utils.getDatabaseDao(applicationContext)
            _resultData.value = resultDao.getAll()
        }
    }

}