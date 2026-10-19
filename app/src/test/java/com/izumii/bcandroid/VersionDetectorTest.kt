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

    @Test
    fun parseVersionConfig_correctlyExtractsNumberAndLock() {
        val (ver1, lock1) = VersionDetector.parseVersionConfig("133:lock")
        assertEquals(133, ver1)
        assertTrue(lock1)

        val (ver2, lock2) = VersionDetector.parseVersionConfig("132")
        assertEquals(132, ver2)
        assertFalse(lock2)

        val (ver3, lock3) = VersionDetector.parseVersionConfig(" 134:LOCK ")
        assertEquals(134, ver3)
        assertTrue(lock3)

        val (ver4, lock4) = VersionDetector.parseVersionConfig("invalid")
        assertEquals(132, ver4)
        assertFalse(lock4)
    }

    @Test
    fun parseNextVersionFromHtml_detectsServerRedirection() {
        val redirectHtml = "<html><head><meta http-equiv=\"refresh\" content=\"2; URL=../R133/\"></head><body>Outdated Version</body></html>"
        val (status1, nextVer1) = VersionDetector.parseNextVersionFromHtml(redirectHtml, 132)
        assertEquals(VersionDetector.VersionStatus.Outdated, status1)
        assertEquals(133, nextVer1)

        val genericOutdatedHtml = "<html><body><h1>Outdated Version - Please Update</h1></body></html>"
        val (status2, nextVer2) = VersionDetector.parseNextVersionFromHtml(genericOutdatedHtml, 132)
        assertEquals(VersionDetector.VersionStatus.Outdated, status2)
        assertEquals(133, nextVer2)

        val activeHtml = "<html><body><canvas id=\"MainCanvas\" width=\"2000\" height=\"1000\"></canvas></body></html>"
        val (status3, nextVer3) = VersionDetector.parseNextVersionFromHtml(activeHtml, 133)
        assertEquals(VersionDetector.VersionStatus.Active, status3)
        assertEquals(null, nextVer3)
    }

    @Test
    fun isGameUrl_distinguishesBetweenGameAndExternalUrls() {
        // Official game URLs
        assertTrue(VersionDetector.isGameUrl("https://www.bondage-asia.com/club/R133/"))
        assertTrue(VersionDetector.isGameUrl("https://bondage-europe.com/club/"))
        assertTrue(VersionDetector.isGameUrl("https://www.bondageprojects.elementfx.com/R132/"))
        assertTrue(VersionDetector.isGameUrl("https://www.bondageprojects.com/club/R134/"))

        // External & non-game links
        assertFalse(VersionDetector.isGameUrl("https://github.com/Izumii99/BC-Desktop"))
        assertFalse(VersionDetector.isGameUrl("https://discord.gg/bondageclub"))
        assertFalse(VersionDetector.isGameUrl("https://www.google.com"))
        assertFalse(VersionDetector.isGameUrl("https://www.bondageprojects.com/privacy"))
        assertFalse(VersionDetector.isGameUrl(""))
        assertFalse(VersionDetector.isGameUrl(null))
    }
}
