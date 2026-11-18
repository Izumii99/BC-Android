package com.izumii.bcandroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ClipboardImageBridgeTest {

    @Test
    fun normalizeImageMime_acceptsRasterFormatsAndRejectsNonRaster() {
        assertEquals("image/png", ClipboardImageBridge.normalizeImageMime("image/png; charset=utf-8", null))
        assertEquals("image/jpeg", ClipboardImageBridge.normalizeImageMime("image/jpg", null))
        assertEquals("image/webp", ClipboardImageBridge.normalizeImageMime("IMAGE/WEBP", null))

        // SVG and plain text must be rejected cleanly
        assertNull(ClipboardImageBridge.normalizeImageMime("image/svg+xml", null))
        assertNull(ClipboardImageBridge.normalizeImageMime("text/plain", null))
    }

    @Test
    fun sniffImageMime_identifiesCommonImageHeaders() {
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)
        val jpgHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val gifHeader = "GIF89a".toByteArray()
        val webpHeader = "RIFF1234WEBP".toByteArray()
        val invalidHeader = "<html>".toByteArray()

        assertEquals("image/png", ClipboardImageBridge.normalizeImageMime("image/*", pngHeader))
        assertEquals("image/jpeg", ClipboardImageBridge.sniffImageMime(jpgHeader))
        assertEquals("image/gif", ClipboardImageBridge.sniffImageMime(gifHeader))
        assertEquals("image/webp", ClipboardImageBridge.sniffImageMime(webpHeader))
        assertNull(ClipboardImageBridge.sniffImageMime(invalidHeader))
    }

    @Test
    fun readBoundedBytes_enforcesMaxLimit() {
        val small = ByteArray(16) { 0x42 }
        val smallResult = ClipboardImageBridge.readBoundedBytes(ByteArrayInputStream(small), 32)
        assertFalse(smallResult.tooLarge)
        assertNotNull(smallResult.bytes)
        assertEquals(16, smallResult.bytes?.size)

        val oversized = ByteArray(40) { 0x42 }
        val largeResult = ClipboardImageBridge.readBoundedBytes(ByteArrayInputStream(oversized), 32)
        assertTrue(largeResult.tooLarge)
        assertNull(largeResult.bytes)
    }

    @Test
    fun buildPayloadJson_andPendingStash_roundTripAndClear() {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val jsonStr = ClipboardImageBridge.buildPayloadJson("image/png", pngBytes)
        val parsed = JSONObject(jsonStr)
        assertEquals("image/png", parsed.getString("mime"))
        assertTrue(parsed.getString("name").endsWith(".png"))
        assertTrue(parsed.getString("dataUrl").startsWith("data:image/png;base64,"))

        ClipboardImageBridge.stashPendingPayload(jsonStr)
        assertEquals(jsonStr, ClipboardImageBridge.consumePendingPayload())
        // Consumed payload must clear immediately to release memory
        assertEquals("", ClipboardImageBridge.consumePendingPayload())
    }
}
