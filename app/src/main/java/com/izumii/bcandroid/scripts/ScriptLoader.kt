package com.izumii.bcandroid.scripts

import android.content.Context
import android.webkit.WebView
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader

object ScriptLoader {

    // Remote loader pointing to Izumii99/BC-Desktop GitHub Pages
    const val REMOTE_MANAGER_URL = "https://izumii99.github.io/BC-Desktop/Scripts/addon-manager.js"

    @Suppress("TooGenericExceptionCaught")
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
        if (webView == null) {
            return
        }

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

    private fun parseConfigJson(rawJson: String?): JSONObject {
        val configStr = if (rawJson != null && rawJson != "null") {
            try {
                JSONTokener(rawJson).nextValue() as? String ?: rawJson
            } catch (e: Exception) {
                rawJson
            }
        } else {
            "{}"
        }

        return try {
            JSONObject(configStr)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun isAddonCandidateEnabled(
        configObj: JSONObject,
        configKeys: List<String>,
        defaultEnabled: Set<String>
    ): Boolean {
        var isEnabled = false
        var hasExplicitConfig = false
        for (key in configKeys) {
            if (configObj.has(key)) {
                hasExplicitConfig = true
                if (configObj.optBoolean(key, false)) {
                    isEnabled = true
                    break
                }
            }
        }

        if (!hasExplicitConfig) {
            isEnabled = configKeys.any { defaultEnabled.contains(it) }
        }
        return isEnabled
    }

    /**
     * Fallback offline injector: loads embedded assets/addons if the remote CDN cannot be reached.
     * Respects user addon preferences saved in localStorage (BCDesktop_Addons_Config).
     */
    @Suppress("TooGenericExceptionCaught")
    fun injectLocalAddons(context: Context, webView: WebView?) {
        if (webView == null) {
            return
        }

        val checkConfigJs = """
            (function() {
                try {
                    return localStorage.getItem('BCDesktop_Addons_Config') || '{}';
                } catch(e) {
                    return '{}';
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(checkConfigJs) { rawJson ->
            val configObj = parseConfigJson(rawJson)
            val defaultEnabled = setOf("chat-qol.js")

            // Ordered scripts without deprecated wardrobe-pagination.js
            val scriptCandidates = listOf(
                "fusam-liko-loader.js" to listOf("FUSAM", "LikoPlugin", "fusam-liko-loader.js"),
                "chat-qol.js" to listOf("chat-qol.js"),
                "translate.js" to listOf("translate.js"),
                "cheat-menu.js" to listOf("cheat-menu.js"),
                "neko-dark.js" to listOf("neko-dark.js")
            )

            try {
                val available = context.assets.list("addons")?.toSet() ?: emptySet()
                for ((filename, configKeys) in scriptCandidates) {
                    if (!available.contains(filename)) {
                        continue
                    }

                    if (isAddonCandidateEnabled(configObj, configKeys, defaultEnabled)) {
                        readAsset(context, "addons/$filename")?.let { addonScript ->
                            webView.evaluateJavascript(addonScript, null)
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
