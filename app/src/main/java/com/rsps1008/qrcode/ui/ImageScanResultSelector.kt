package com.rsps1008.qrcode.ui

internal object ImageScanResultSelector {
    fun <T> selectPrimaryReadableResult(
        results: List<T>,
        content: (T) -> String?,
        area: (T) -> Long
    ): T? {
        val primaryResult = results.maxByOrNull(area) ?: return null
        return primaryResult.takeIf { isReadableContent(content(it)) }
    }

    internal fun isReadableContent(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        return content.none { character ->
            character == REPLACEMENT_CHARACTER ||
                (character.isISOControl() && character !in ALLOWED_CONTROL_CHARACTERS)
        }
    }

    private const val REPLACEMENT_CHARACTER = '\uFFFD'
    private val ALLOWED_CONTROL_CHARACTERS = setOf('\n', '\r', '\t')
}
