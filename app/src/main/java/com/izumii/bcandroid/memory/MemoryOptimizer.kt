package com.izumii.bcandroid.memory

import android.content.ComponentCallbacks2
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView

object MemoryOptimizer {

    // Throttles redraws when app is unfocused/backgrounded to minimize CPU & RAM heat
    private const val CAP_UNFOCUSED_FPS_SCRIPT =
        "(()=>{" +
        "const g=window.Player&&Player.GraphicsSettings;" +
        "if(g&&!g.MaxUnfocusedFPS&&window.CurrentScreen!=='Preference'){g.MaxUnfocusedFPS=15;window.bcFpsCap=g;}" +
        "if(window.bcFocusMemory&&!window.bcFocusMemory.isConnected)window.bcFocusMemory=null;" +
        "if(window.bcLastMousedown&&!window.bcLastMousedown.isConnected)window.bcLastMousedown=null;" +
        "if(window.bcLastFocusedLayer&&!window.bcLastFocusedLayer.isConnected)window.bcLastFocusedLayer=null;" +
        "if(typeof window.gc==='function'){try{window.gc();}catch(e){}}" +
        "})();"

    private const val RESTORE_UNFOCUSED_FPS_SCRIPT =
        "(()=>{const g=window.bcFpsCap;if(g){if(g.MaxUnfocusedFPS===15)g.MaxUnfocusedFPS=0;window.bcFpsCap=null;}})();"

    /**
     * Called when Activity is sent to background or loses focus.
     * Throttles graphics engine to 15 FPS and trims volatile caches without pausing
     * background JS timers or dropping Socket.IO WebSocket room connections.
     */
    fun onPause(webView: WebView?) {
        webView ?: return

        // 1. Immediately throttle in-game graphics engine to 15 FPS to cut CPU heat and battery
        webView.evaluateJavascript(CAP_UNFOCUSED_FPS_SCRIPT, null)

        // 2. Prune volatile image/texture caches to release RAM (keeps persistent storage intact)
        try {
            webView.clearCache(false)
            val freeMemMethod = WebView::class.java.getMethod("freeMemory")
            freeMemMethod.invoke(webView)
        } catch (ignored: Exception) {
        }
    }

    /**
     * Called when Activity returns to foreground.
     */
    fun onResume(webView: WebView?) {
        webView ?: return

        // Restore normal game FPS
        webView.evaluateJavascript(RESTORE_UNFOCUSED_FPS_SCRIPT, null)
    }

    /**
     * Reacts to OS memory pressure signals (ComponentCallbacks2).
     */
    fun onTrimMemory(webView: WebView?, level: Int) {
        webView ?: return

        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Moderate pressure: clear volatile image caches
                try {
                    webView.clearCache(false)
                } catch (ignored: Exception) {
                }
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // High pressure: trim memory and suggest runtime garbage collection
                try {
                    webView.clearCache(false)
                    // Invoke freeMemory if available on this WebKit build
                    val freeMemMethod = WebView::class.java.getMethod("freeMemory")
                    freeMemMethod.invoke(webView)
                } catch (ignored: Exception) {
                }
                System.gc()
            }
        }
    }

    /**
     * Complete teardown of WebView to avoid memory leaks.
     */
    fun destroyWebView(webView: WebView?) {
        webView ?: return
        try {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        } catch (ignored: Exception) {
        }
    }
}
