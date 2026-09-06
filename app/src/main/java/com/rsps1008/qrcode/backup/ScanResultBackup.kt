package com.rsps1008.qrcode.backup

import com.rsps1008.qrcode.ui.database.ScanResult
import com.rsps1008.qrcode.ui.database.TYPE
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/** Versioned JSON representation used for the Google Drive appDataFolder backup. */
object ScanResultBackup {
    const val FILE_NAME = "qrcode_scanner_history.json"
    private const val VERSION = 1
    private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    private const val MAX_RESULTS = 100_000

    fun encode(results: List<ScanResult>): ByteArray {
        require(results.size <= MAX_RESULTS) { "掃描歷史筆數過多" }

        val items = JSONArray()
        results.forEach { result ->
            items.put(JSONObject().apply {
                put("timestamp", result.timestamp.time)
                put("content", result.content)
                put("type", result.type.name)
                put("title", result.title ?: JSONObject.NULL)
                put("isFavorite", result.isFavorite)
            })
        }

        return JSONObject()
            .put("version", VERSION)
            .put("results", items)
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): List<ScanResult> {
        require(bytes.size <= MAX_BACKUP_BYTES) { "Google Drive 備份檔案過大" }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.optInt("version", -1) == VERSION) { "不支援的 QR Code 備份版本" }
        val items = root.optJSONArray("results") ?: error("備份缺少歷史資料")
        require(items.length() <= MAX_RESULTS) { "備份歷史筆數過多" }

        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val timestamp = item.optLong("timestamp", -1L)
            require(timestamp >= 0L) { "備份含有無效時間" }
            require(item.has("content") && !item.isNull("content")) { "備份含有無效內容" }
            val content = item.getString("content")
            require(item.has("type") && !item.isNull("type")) { "備份缺少內容類型" }
            val typeName = item.getString("type")
            val title = if (item.isNull("title")) null else item.getString("title")
            val isFavorite = item.optBoolean("isFavorite", false)

            ScanResult(
                id = 0,
                timestamp = Date(timestamp),
                content = content,
                type = try {
                    TYPE.valueOf(typeName)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("備份含有不支援的內容類型", e)
                },
                title = title,
                isFavorite = isFavorite
            )
        }
    }
}
