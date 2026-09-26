package com.rsps1008.qrcode.ui

import kotlin.math.ceil
import kotlin.math.max

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

    internal fun calculateQuietZonePadding(width: Int, height: Int): Int {
        return ceil(max(width, height) * QUIET_ZONE_RATIO).toInt()
            .coerceAtLeast(MIN_QUIET_ZONE_PADDING_PX)
    }

    private const val REPLACEMENT_CHARACTER = '\uFFFD'
    private const val QUIET_ZONE_RATIO = 0.25
    private const val MIN_QUIET_ZONE_PADDING_PX = 24
    private val ALLOWED_CONTROL_CHARACTERS = setOf('\n', '\r', '\t')
}
