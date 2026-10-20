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
    }
}
