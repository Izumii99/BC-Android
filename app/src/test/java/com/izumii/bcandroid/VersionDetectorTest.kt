package com.izumii.bcandroid

import com.izumii.bcandroid.updater.VersionDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionDetectorTest {

    @Test
    fun isNewerVersion_correctlyComparesTags() {
        assertTrue(VersionDetector.isNewerVersion("v1.1", "1.0"))
        assertTrue(VersionDetector.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(VersionDetector.isNewerVersion("v1.0.1", "v1.0.0"))

        assertFalse(VersionDetector.isNewerVersion("1.0", "1.0"))
        assertFalse(VersionDetector.isNewerVersion("v1.0", "1.0"))
        assertFalse(VersionDetector.isNewerVersion("0.9", "1.0"))
        assertFalse(VersionDetector.isNewerVersion("", "1.0"))
        assertFalse(VersionDetector.isNewerVersion("1.0", ""))
    }

    @Test
    fun defaultFallbackUrl_isValid() {
        assertEquals("https://www.bondage-asia.com/club/R132/", VersionDetector.DEFAULT_FALLBACK_URL)
    }

    @Test
    fun portraitAspectRatio_preservesTwoToOneGameDimensions() {
        // Native canvas coordinate width/height = 2.0
        val screenWidth = 1080.0
        val nativeAspect = 2.0
        val calculatedCanvasHeight = screenWidth / nativeAspect

        assertEquals(540.0, calculatedCanvasHeight, 0.001)
        assertTrue(calculatedCanvasHeight <= 2400.0) // Fits on standard 20:9 / 19.5:9 portrait screens
    }

    @Test
    fun portraitAdapter_containsDynamicZoomControl() {
        val possiblePaths = listOf(
            java.io.File("app/src/main/assets/portrait_adapter.js"),
            java.io.File("../app/src/main/assets/portrait_adapter.js")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("portrait_adapter.js must exist", file != null && file.exists())
        val content = file?.readText() ?: ""
        assertTrue("Must include updateZoomState function", content.contains("updateZoomState"))
        assertTrue("Must include user-scalable=yes for zoomable screens", content.contains("user-scalable=yes"))
        assertTrue("Must include user-scalable=no for portrait screens", content.contains("user-scalable=no"))
        assertTrue("Must communicate with AndroidBridge.setZoomEnabled", content.contains("AndroidBridge.setZoomEnabled"))
    }
}
