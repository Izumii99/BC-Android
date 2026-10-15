package com.izumii.bcandroid.scripts

import android.content.Context
import android.webkit.WebView
import java.io.BufferedReader
import java.io.InputStreamReader

object ScriptLoader {

    // Remote loader pointing to Izumii99/BC-Desktop
    const val REMOTE_MANAGER_URL = "https://cdn.jsdelivr.net/gh/Izumii99/BC-Desktop@main/Scripts/addon-manager.js"

    private fun readAsset(context: Context, assetPath: String): String? {
        return try {
            context.assets.open(assetPath).use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Injects the portrait adapter, desktop bridge, and the remote addon loader from BC-Desktop into the game DOM.
     */
    fun injectCoreScripts(context: Context, webView: WebView?) {
        webView ?: return

        // 1. Portrait scaling & responsive viewport adapter
        readAsset(context, "portrait_adapter.js")?.let { script ->
            webView.evaluateJavascript(script, null)
        }

        // 2. Desktop clipboard & smart focus bridge
        readAsset(context, "scripts/desktop_bridge.js")?.let { script ->
            webView.evaluateJavascript(script, null)
        }

        // 3. Remote Addon Loader (pulls directly from Izumii99/BC-Desktop repo)
        readAsset(context, "scripts/inject-all.js")?.let { remoteLoaderScript ->
            webView.evaluateJavascript(remoteLoaderScript, null)
        }
    }

    /**
     * Fallback offline injector: loads embedded assets/addons if the remote CDN cannot be reached.
     */
    fun injectLocalAddons(context: Context, webView: WebView?) {
        webView ?: return

        val orderedScripts = listOf(
            "fusam-liko-loader.js",
            "chat-qol.js",
            "translate.js",
            "cheat-menu.js",
            "neko-dark.js",
            "wardrobe-pagination.js"
        )

        try {
            val available = context.assets.list("addons")?.toSet() ?: emptySet()
            for (scriptName in orderedScripts) {
                if (available.contains(scriptName)) {
                    readAsset(context, "addons/$scriptName")?.let { addonScript ->
                        webView.evaluateJavascript(addonScript, null)
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }
}
