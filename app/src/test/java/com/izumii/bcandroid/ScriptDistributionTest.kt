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
    }
}
