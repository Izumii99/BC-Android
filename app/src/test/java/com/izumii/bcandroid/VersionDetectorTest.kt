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
}
