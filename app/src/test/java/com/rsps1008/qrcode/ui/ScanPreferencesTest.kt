package com.rsps1008.qrcode.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ScanPreferencesTest {
    @Test
    fun scanPreferenceDefaultsMatchFirstInstallContractAndSettingsUi() {
        val expectedDefaults = mapOf(
            "close_after_scan" to false,
            "auto_add_wifi" to false,
            "auto_open_url" to false,
            "auto_open_actions" to false,
            "auto_copy_text" to true,
            "vibrate_when_copy_text_success" to true
        )

        assertEquals(expectedDefaults, ScanPreferences.defaults)
        assertEquals(expectedDefaults, readSwitchDefaultsFromSettingsXml())

        val expectedStringDefaults = mapOf("image_source" to "photo_picker")
        assertEquals(expectedStringDefaults, ScanPreferences.stringDefaults)
        assertEquals(
            expectedStringDefaults,
            readListDefaultsFromSettingsXml(expectedStringDefaults.keys)
        )
    }

    private fun readSwitchDefaultsFromSettingsXml(): Map<String, Boolean> {
        val document = readSettingsDocument()
        val switches = document.getElementsByTagName("SwitchPreferenceCompat")

        return buildMap {
            for (index in 0 until switches.length) {
                val element = switches.item(index)
                val key = element.attributes
                    .getNamedItemNS(ANDROID_NAMESPACE, "key")
                    .nodeValue
                val defaultValue = element.attributes
                    .getNamedItemNS(ANDROID_NAMESPACE, "defaultValue")
                    .nodeValue
                    .toBooleanStrict()
                put(key, defaultValue)
            }
        }
    }

    private fun readListDefaultsFromSettingsXml(keys: Set<String>): Map<String, String> {
        val lists = readSettingsDocument().getElementsByTagName("ListPreference")

        return buildMap {
            for (index in 0 until lists.length) {
                val element = lists.item(index)
                val key = element.attributes
                    .getNamedItemNS(ANDROID_NAMESPACE, "key")
                    .nodeValue
                if (key in keys) {
                    put(
                        key,
                        element.attributes
                            .getNamedItemNS(ANDROID_NAMESPACE, "defaultValue")
                            .nodeValue
                    )
                }
            }
        }
    }

    private fun readSettingsDocument() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(settingsFile())

    private fun settingsFile(): File {
        val settingsFile = sequenceOf(
            File("src/main/res/xml/preference_main.xml"),
            File("app/src/main/res/xml/preference_main.xml")
        ).first(File::isFile)
        return settingsFile
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
