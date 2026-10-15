package com.izumii.bcandroid.scripts

import android.content.Context
import android.webkit.WebView
import java.io.BufferedReader
import java.io.InputStreamReader

object ScriptLoader {

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
     * Injects the portrait adapter and desktop bridge scripts into the game DOM.
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

        // 3. Inject any bundled addons in assets/addons/
        try {
            val addonFiles = context.assets.list("addons") ?: emptyArray()
            for (file in addonFiles) {
                if (file.endsWith(".js")) {
                    readAsset(context, "addons/$file")?.let { addonScript ->
                        webView.evaluateJavascript(addonScript, null)
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }
}
