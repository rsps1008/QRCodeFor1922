package com.rsps1008.qrcode

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import com.rsps1008.qrcode.ui.MainActivity.Companion.PREFKEY


class SettingsPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        preferenceManager.sharedPreferencesName = PREFKEY
        setPreferencesFromResource(R.xml.preference_main, rootKey)
        findPreference<SwitchPreferenceCompat>("dark_mode")?.setOnPreferenceChangeListener { _, value ->
            AppCompatDelegate.setDefaultNightMode(
                if (value == true) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundResource(R.color.preference_bg_color)
        view.findViewById<RecyclerView>(android.R.id.list)?.apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding)
            clipToPadding = false
            setBackgroundResource(R.color.preference_bg_color)
        }
    }
}
