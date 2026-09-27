package com.rsps1008.qrcode.ui

import android.content.SharedPreferences

internal object ScanPreferences {
    const val CLOSE_AFTER_SCAN = "close_after_scan"
    const val AUTO_ADD_WIFI = "auto_add_wifi"
    const val AUTO_OPEN_URL = "auto_open_url"
    const val AUTO_OPEN_ACTIONS = "auto_open_actions"
    const val AUTO_COPY_TEXT = "auto_copy_text"
    const val COPY_TEXT_VIBRATE = "vibrate_when_copy_text_success"
    const val IMAGE_SOURCE = "image_source"
    const val IMAGE_SOURCE_CHOICE_CONFIRMED = "image_source_choice_confirmed"

    const val IMAGE_SOURCE_PHOTO_PICKER = "photo_picker"
    const val IMAGE_SOURCE_FILE_BROWSER = "file_browser"

    const val DEFAULT_CLOSE_AFTER_SCAN = false
    const val DEFAULT_AUTO_ADD_WIFI = false
    const val DEFAULT_AUTO_OPEN_URL = false
    const val DEFAULT_AUTO_OPEN_ACTIONS = false
    const val DEFAULT_AUTO_COPY_TEXT = true
    const val DEFAULT_COPY_TEXT_VIBRATE = true
    const val DEFAULT_IMAGE_SOURCE = IMAGE_SOURCE_PHOTO_PICKER

    internal val defaults = mapOf(
        CLOSE_AFTER_SCAN to DEFAULT_CLOSE_AFTER_SCAN,
        AUTO_ADD_WIFI to DEFAULT_AUTO_ADD_WIFI,
        AUTO_OPEN_URL to DEFAULT_AUTO_OPEN_URL,
        AUTO_OPEN_ACTIONS to DEFAULT_AUTO_OPEN_ACTIONS,
        AUTO_COPY_TEXT to DEFAULT_AUTO_COPY_TEXT,
        COPY_TEXT_VIBRATE to DEFAULT_COPY_TEXT_VIBRATE
    )

    internal val stringDefaults = mapOf(
        IMAGE_SOURCE to DEFAULT_IMAGE_SOURCE
    )

    fun initializeMissingDefaults(preferences: SharedPreferences) {
        val missingDefaults = defaults.filterKeys { key -> !preferences.contains(key) }
        val missingStringDefaults = stringDefaults.filterKeys { key -> !preferences.contains(key) }
        if (missingDefaults.isEmpty() && missingStringDefaults.isEmpty()) return

        preferences.edit().apply {
            missingDefaults.forEach { (key, value) -> putBoolean(key, value) }
            missingStringDefaults.forEach { (key, value) -> putString(key, value) }
        }.apply()
    }
}
