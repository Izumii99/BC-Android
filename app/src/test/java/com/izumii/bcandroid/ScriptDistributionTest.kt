package com.izumii.bcandroid

import com.izumii.bcandroid.scripts.ScriptLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ScriptDistributionTest {

    @Test
    fun remoteManagerUrl_targetsBcDesktopRepository() {
        assertEquals(
            "https://izumii99.github.io/BC-Desktop/Scripts/addon-manager.js",
            ScriptLoader.REMOTE_MANAGER_URL
        )
    }

    @Test
    fun publishLoader_injectAllScriptExistsAndValidates() {
        // Look for loader.user.js or assets/scripts relative to project root
        val possiblePaths = listOf(
            File("loader.user.js"),
            File("../loader.user.js"),
            File("app/src/main/assets/scripts/inject-all.js"),
            File("../app/src/main/assets/scripts/inject-all.js")
        )

        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("inject-all.js or loader.user.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("inject-all.js must reference BC-Desktop addon-manager.js", content.contains("BC-Desktop@main/Scripts/addon-manager.js"))
        assertTrue("inject-all.js must include singleton guard", content.contains("_bcRemoteLoaderInjected"))
        assertTrue("inject-all.js must trigger offline fallback on bridge", content.contains("loadOfflineAddons"))
    }

    @Test
    fun desktopBridge_containsPidxGuardAndFocusHooks() {
        val possiblePaths = listOf(
            File("app/src/main/assets/scripts/desktop_bridge.js"),
            File("../app/src/main/assets/scripts/desktop_bridge.js")
        )

        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("desktop_bridge.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("desktop_bridge.js must include pidx guard", content.contains("typeof window.pidx === 'undefined'"))
        assertTrue("desktop_bridge.js must hook ElementFocus", content.contains("window.ElementFocus"))
        assertTrue("desktop_bridge.js must protect interactive elements from InputChat focus theft", content.contains("isInteractive"))
    }

    @Test
    fun titleBadgeRegex_correctlyParsesNotificationCount() {
        val regex = Regex("""^\((\d+)\)""")
        val match1 = regex.find("(1) Bondage Club")
        assertEquals(1, match1?.groupValues?.get(1)?.toIntOrNull())

        val match99 = regex.find("(99) Bondage Club")
        assertEquals(99, match99?.groupValues?.get(1)?.toIntOrNull())

        val matchNone = regex.find("Bondage Club")
        assertEquals(null, matchNone?.groupValues?.get(1)?.toIntOrNull())
    }

    @Test
    fun desktopBridge_containsVisibilitySpoofAndAudioKeepAlive() {
        val possiblePaths = listOf(
            File("app/src/main/assets/scripts/desktop_bridge.js"),
            File("../app/src/main/assets/scripts/desktop_bridge.js")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("desktop_bridge.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("Must spoof Page Visibility API", content.contains("visibilityState") && content.contains("document, 'hidden'"))
        assertTrue("Must include silent WebAudio keep-alive", content.contains("initSilentAudioKeepAlive") && content.contains("_bcAudioKeepAliveActive"))
    }

    @Test
    fun desktopBridge_containsDownloadAndFileExportHooks() {
        val possiblePaths = listOf(
            File("app/src/main/assets/scripts/desktop_bridge.js"),
            File("../app/src/main/assets/scripts/desktop_bridge.js")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("desktop_bridge.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("Must hook anchor click for downloads", content.contains("HTMLAnchorElement.prototype.click"))
        assertTrue("Must handle blob and data URLs", content.contains("saveBase64File") && content.contains("blob:"))
        assertTrue("Must hook revokeObjectURL to prevent race conditions", content.contains("window.URL.revokeObjectURL"))
        assertTrue("Must protect layering inputs from focus theft", content.contains("currentIsLayering"))
    }

    @Test
    fun desktopBridge_containsSafeVramAndTextureOptimization() {
        val possiblePaths = listOf(
            File("app/src/main/assets/scripts/desktop_bridge.js"),
            File("../app/src/main/assets/scripts/desktop_bridge.js")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("desktop_bridge.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("Must define setupSafeVramOptimization", content.contains("setupSafeVramOptimization"))
        assertTrue("Must tune ImageCachePurgeDelay", content.contains("ImageCachePurgeDelay"))
        assertTrue("Must define WARDROBE_PURGE_DELAY", content.contains("WARDROBE_PURGE_DELAY"))
        assertTrue("Must define NORMAL_PURGE_DELAY", content.contains("NORMAL_PURGE_DELAY"))
        assertTrue("Must call safe GLDrawImageCache.purge", content.contains("GLDrawImageCache.purge"))
        assertTrue("Must call safe DrawImageCache.purge", content.contains("DrawImageCache.purge"))
    }

    @Test
    fun desktopBridge_containsTwoWayClipboardAndImportSupport() {
        val possiblePaths = listOf(
            File("app/src/main/assets/scripts/desktop_bridge.js"),
            File("../app/src/main/assets/scripts/desktop_bridge.js")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("desktop_bridge.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("Must bridge readText to AndroidBridge.getClipboardText", content.contains("AndroidBridge.getClipboardText"))
        assertTrue("Must hook execCommand paste", content.contains("commandId.toLowerCase() === 'paste'"))
        assertTrue("Must dispatch paste ClipboardEvent on Ctrl+V", content.contains("new ClipboardEvent('paste'") && content.contains("KeyV"))
    }

    @Test
    fun desktopBridge_containsPidxGuardAndClipboardBridge() {
        val possiblePaths = listOf(
            File("app/src/main/assets/scripts/desktop_bridge.js"),
            File("../app/src/main/assets/scripts/desktop_bridge.js")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("desktop_bridge.js must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("Must include pidx guard", content.contains("window.pidx"))
        assertTrue("Must include clipboard bridge", content.contains("navigator.clipboard"))
    }

    @Test
    fun memoryOptimizer_containsHardenedHooksAndGcCooldown() {
        val possiblePaths = listOf(
            File("app/src/main/java/com/izumii/bcandroid/memory/MemoryOptimizer.kt"),
            File("../app/src/main/java/com/izumii/bcandroid/memory/MemoryOptimizer.kt")
        )
        val file = possiblePaths.firstOrNull { it.exists() }
        assertTrue("MemoryOptimizer.kt must exist", file != null && file.exists())

        val content = file?.readText() ?: ""
        assertTrue("Must intercept document.hasFocus", content.contains("__bcFocusOverridden"))
        assertTrue("Must use idempotent permanent DrawProcess hook", content.contains("__bcDrawHooked"))
        assertTrue("Must support 15 FPS split-screen background throttling", content.contains("_lastBgDrawTime") && content.contains("<66"))
        assertTrue("Must support complete 0 FPS minimization guard", content.contains("window._bcIsMinimized"))
        assertTrue("Must enforce GC cooldown", content.contains("_bcLastGc") && content.contains("60000"))
    }
}
