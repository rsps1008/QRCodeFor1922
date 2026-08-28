package com.rsps1008.qrcode.ui

import android.R
import android.app.Application
import android.content.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.rsps1008.qrcode.Utils.getDatabaseDao
import com.rsps1008.qrcode.ui.MainActivity.Companion.PREFKEY
import com.rsps1008.qrcode.ui.database.ScanResult
import com.rsps1008.qrcode.ui.database.TYPE
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.Html
import android.net.wifi.WifiNetworkSuggestion
import android.provider.Settings
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var mLastTriggerText: String = ""
    private var mHasTrigger = false
    private var bAgreement = false
    private var bRedirectDialogShowing = false
    private var bSettingsShow = false
    private var mTempIntent: Intent? = null

    private val _showAgreement = MutableLiveData<Boolean?>()
    private val _startCamera = MutableLiveData<Boolean?>()
    private val _copyAlready = MutableLiveData<String>()
    private val _startActivity = MutableLiveData<Intent?>()
    private val _finishActivity = MutableLiveData<Boolean?>()
    private val _showDetectOtherDialog = MutableLiveData<Barcode?>()
    private val _showHistoryPrompt = MutableLiveData<Boolean?>()
    private val _vibrate = MutableLiveData<Boolean?>()
    val showAgreement:LiveData<Boolean?> = _showAgreement
    val startCamera:LiveData<Boolean?> = _startCamera
    val copyAlready:LiveData<String> = _copyAlready
    val startActivity:LiveData<Intent?> = _startActivity
    val finishActivity:LiveData<Boolean?> = _finishActivity
    val showDetectOtherDialog:LiveData<Barcode?> = _showDetectOtherDialog
    val showHistoryPrompt:LiveData<Boolean?> = _showHistoryPrompt
    val vibrate:LiveData<Boolean?>  = _vibrate

    private lateinit var mPref: SharedPreferences
    private val mHandler = Handler(Looper.getMainLooper())

    init {
        try {
            mPref = application.getSharedPreferences(PREFKEY, AppCompatActivity.MODE_PRIVATE)
        } catch (e: Exception) {
        }

    }

    fun ready() {
        bAgreement = mPref.getBoolean(AGREEMENT, false)
        val historyPrompt = mPref.getBoolean(FEATURE_HISTORY, false)

        if (!bAgreement) {
            _showAgreement.value = true
        } else if (!historyPrompt)
            viewModelScope.launch {
                delay(1000)
                _showHistoryPrompt.value = !historyPrompt
            }
        else {
            _startCamera.value = true
        }
    }

    fun userAgree() {
        mPref.edit().putBoolean(AGREEMENT, true).apply()
        ready()
    }

    fun confirmNewFeature() {
        mPref.edit().putBoolean(FEATURE_HISTORY, true).apply()
        ready()
    }

    fun startWithHttp(content: String): Boolean {
        return (content.startsWith("http://") or content.startsWith("https://") or content.startsWith("www"))
    }

    private fun triggerBarcode(barcode: Barcode) {
        if (barcode.rawValue == null || mHasTrigger ||
            TextUtils.equals(mLastTriggerText, barcode.rawValue)
            || bSettingsShow || bRedirectDialogShowing
        ) {
            return
        }

        if (barcode.valueType == Barcode.TYPE_SMS) {
            val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${barcode.sms?.phoneNumber.orEmpty()}")
                putExtra("sms_body", barcode.sms?.message.orEmpty())
            }
            handleActionIntent(barcode, sendIntent)
            saveResultToDb(barcode.rawValue ?: barcode.sms?.message, TYPE.SMS)
        } else {
            if (barcode.valueType == Barcode.TYPE_WIFI
                && mPref.getBoolean(PREF_AUTO_ADD_WIFI, true)
            ) {
                buildWifiSetupIntent(barcode)?.let { intent ->
                    _startActivity.value = intent
                } ?: copyToClipboard(barcode.rawValue ?: "")
                saveResultToDb(barcode.rawValue, TYPE.WIFI)
            } else if (barcode.valueType == Barcode.TYPE_PHONE) {
                handleActionIntent(
                    barcode,
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${barcode.phone?.number.orEmpty()}"))
                )
                saveResultToDb(barcode.rawValue, TYPE.PHONE)
            } else if (barcode.valueType == Barcode.TYPE_EMAIL) {
                val email = barcode.email
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email?.address.orEmpty()}"))
                    .apply {
                        putExtra(Intent.EXTRA_SUBJECT, email?.subject.orEmpty())
                        putExtra(Intent.EXTRA_TEXT, email?.body.orEmpty())
                    }
                handleActionIntent(barcode, emailIntent)
                saveResultToDb(barcode.rawValue, TYPE.EMAIL)
            } else {
                val rawValue = barcode.rawValue.orEmpty()
                val isUrl = barcode.valueType == Barcode.TYPE_URL || startWithHttp(rawValue)
                if (isUrl) {
                    synchronized(obj) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawValue))
                        if (mPref.getBoolean(PREF_AUTO_OPEN_URL, false)) {
                            _startActivity.value = intent
                        } else if (getApplication<Application>().packageManager.resolveActivity(intent, 0) != null) {
                            mTempIntent = intent
                            _showDetectOtherDialog.value = barcode
                            _showDetectOtherDialog.value = null
                            bRedirectDialogShowing = true
                        } else {
                            copyToClipboard(rawValue)
                        }
                    }
                    saveResultToDb(
                        rawValue,
                        TYPE.REDIRECT,
                        finishAfterTitle = mPref.getBoolean(PREF_CLOSE_APP_AFTER_SCAN, false)
                            && mPref.getBoolean(PREF_AUTO_OPEN_URL, false)
                    )
                } else {
                    copyToClipboard(rawValue)
                    saveResultToDb(rawValue, TYPE.TEXT)
                }
            }
        }
        mHandler.postDelayed(Runnable {
            mLastTriggerText = ""
            mHasTrigger = false
        }, 1500)
        mLastTriggerText = barcode.rawValue ?: ""
        mHasTrigger = true
    }

    private fun handleActionIntent(barcode: Barcode, intent: Intent) {
        if (mPref.getBoolean(PREF_AUTO_OPEN_ACTIONS, false)) {
            _startActivity.value = intent
            if (mPref.getBoolean(PREF_CLOSE_APP_AFTER_SCAN, false)) {
                _finishActivity.value = true
            }
        } else if (getApplication<Application>().packageManager.resolveActivity(intent, 0) != null) {
            synchronized(obj) {
                mTempIntent = intent
                _showDetectOtherDialog.value = barcode
                _showDetectOtherDialog.value = null
                bRedirectDialogShowing = true
            }
        } else {
            copyToClipboard(barcode.rawValue ?: "")
        }
    }

    fun newBarcodes(barcodes: List<Barcode>) {
        if (barcodes.isNotEmpty()) {
            triggerBarcode(barcodes.first())
        }
    }

    fun copyToClipboard(text: String) {
        if (mPref.getBoolean(PREF_AUTO_COPY_TEXT, true)) {
            if (mPref.getBoolean(PREF_COPY_TEXT_VIBRATE, true)) {
                _vibrate.value = true
            }
            val clipboardManager =
                getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("simple text", text)
            clipboardManager.setPrimaryClip(clip)
            _copyAlready.value = text
            _copyAlready.value = ""
        }
    }

    fun isSettingsShowing(show: Boolean) {
        bSettingsShow = show
    }

    fun activeIntent() {
        _startActivity.value = mTempIntent
        _startActivity.value = null
    }

    fun resetRedirectDialog() {
        bRedirectDialogShowing = false
    }

    fun resetVibrate() {
        _vibrate.value = null
    }

    private fun buildWifiSetupIntent(barcode: Barcode): Intent? {
        val wifi = barcode.wifi ?: return null
        val ssid = wifi.ssid?.takeIf(String::isNotBlank) ?: return null
        val suggestionBuilder = WifiNetworkSuggestion.Builder().setSsid(ssid)

        when (wifi.encryptionType) {
            Barcode.WiFi.TYPE_OPEN -> suggestionBuilder.setIsEnhancedOpen(false)
            Barcode.WiFi.TYPE_WPA -> {
                val password = wifi.password?.takeIf(String::isNotEmpty) ?: return null
                suggestionBuilder.setWpa2Passphrase(password)
            }
            else -> return null
        }

        return Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
            putParcelableArrayListExtra(
                Settings.EXTRA_WIFI_NETWORK_LIST,
                arrayListOf(suggestionBuilder.build())
            )
        }
    }

    fun resetAgreement() {
        _showAgreement.value = null
    }

    fun resetStartCamera() {
        _startCamera.value = null
    }

    fun saveResultToDb(rawValue: String?, type: TYPE, finishAfterTitle: Boolean = false) {
        rawValue?.let {
            saveResultToDbLocked(it, type, finishAfterTitle)
        }
    }

    private fun saveResultToDbLocked(data: String, type: TYPE, finishAfterTitle: Boolean) {
        val resultDao = getDatabaseDao(getApplication())
        viewModelScope.launch {
            val resultId = resultDao.insert(ScanResult(Date(), data, type))
            if (startWithHttp(data)) {
                fetchWebTitle(data)?.let { title ->
                    resultDao.updateTitle(resultId, title)
                }
            }
            if (finishAfterTitle) {
                _finishActivity.value = true
            }
        }
    }

    private suspend fun fetchWebTitle(rawUrl: String): String? = withContext(Dispatchers.IO) {
        val normalizedUrl = if (rawUrl.startsWith("www", ignoreCase = true)) {
            "https://$rawUrl"
        } else {
            rawUrl
        }
        runCatching {
            val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 QRCodeScanner")
            }
            try {
                if (connection.responseCode !in 200..399) return@runCatching null
                val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText().take(MAX_HTML_SIZE)
                }
                TITLE_PATTERN.find(html)?.groupValues?.get(1)
                    ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString() }
                    ?.replace(WHITESPACE_PATTERN, " ")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        private const val AGREEMENT = "agreement"
        private const val FEATURE_HISTORY = "feature_history"
        private const val PREF_CLOSE_APP_AFTER_SCAN = "close_after_scan"
        private const val PREF_AUTO_ADD_WIFI = "auto_add_wifi"
        private const val PREF_AUTO_OPEN_URL = "auto_open_url"
        private const val PREF_AUTO_OPEN_ACTIONS = "auto_open_actions"
        private const val PREF_AUTO_COPY_TEXT = "auto_copy_text"
        private const val PREF_COPY_TEXT_VIBRATE = "vibrate_when_copy_text_success"
        private const val MAX_HTML_SIZE = 512 * 1024
        private val TITLE_PATTERN = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val WHITESPACE_PATTERN = Regex("\\s+")
        private val obj = Object()
    }
}
