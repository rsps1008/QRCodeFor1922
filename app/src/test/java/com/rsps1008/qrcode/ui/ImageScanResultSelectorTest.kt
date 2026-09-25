package com.rsps1008.qrcode.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageScanResultSelectorTest {
    @Test
    fun selectsLargestReadableResultInsteadOfFirstResult() {
        val smallUrl = Candidate("https://example.com", 100L)
        val mainText = Candidate("こんにちは世界", 10_000L)

        val result = ImageScanResultSelector.selectPrimaryReadableResult(
            listOf(smallUrl, mainText),
            content = Candidate::content,
            area = Candidate::area
        )

        assertEquals(mainText, result)
    }

    @Test
    fun rejectsUnreadablePrimaryResultInsteadOfUsingSmallerCode() {
        val smallUrl = Candidate("https://example.com", 100L)
        val unreadableMainCode = Candidate("broken\u0000content", 10_000L)

        val result = ImageScanResultSelector.selectPrimaryReadableResult(
            listOf(smallUrl, unreadableMainCode),
            content = Candidate::content,
            area = Candidate::area
        )

        assertNull(result)
    }

    @Test
    fun rejectsBlankAndReplacementCharacterContent() {
        assertNull(selectSingle("  "))
        assertNull(selectSingle("broken\uFFFDcontent"))
    }

    private fun selectSingle(content: String): Candidate? {
        return ImageScanResultSelector.selectPrimaryReadableResult(
            listOf(Candidate(content, 1L)),
            content = Candidate::content,
            area = Candidate::area
        )
    }

    private data class Candidate(val content: String, val area: Long)
}
