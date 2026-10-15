package com.izumii.bcandroid.memory

import android.content.ComponentCallbacks2
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView

object MemoryOptimizer {

    // Throttles redraws when app is unfocused/backgrounded to minimize CPU & RAM heat
    private const val CAP_UNFOCUSED_FPS_SCRIPT =
        "(()=>{const g=window.Player&&Player.GraphicsSettings;if(g&&!g.MaxUnfocusedFPS&&window.CurrentScreen!=='Preference'){g.MaxUnfocusedFPS=15;window.bcFpsCap=g;}})();"

    private const val RESTORE_UNFOCUSED_FPS_SCRIPT =
        "(()=>{const g=window.bcFpsCap;if(g){if(g.MaxUnfocusedFPS===15)g.MaxUnfocusedFPS=0;window.bcFpsCap=null;}})();"

    private val handler = Handler(Looper.getMainLooper())
    private var backgroundPauseRunnable: Runnable? = null

    /**
     * Called when Activity is paused or sent to background.
     */
    fun onPause(webView: WebView?) {
        webView ?: return

        // Immediately throttle in-game graphics engine to 15 FPS
        webView.evaluateJavascript(CAP_UNFOCUSED_FPS_SCRIPT, null)
        webView.onPause()

        // If backgrounded for more than 5 seconds, pause WebView JS timers to conserve battery and RAM
        backgroundPauseRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            try {
                webView.pauseTimers()
                // Clear in-memory resource caches (keeps persistent localStorage and cookies intact)
                webView.clearCache(false)
            } catch (ignored: Exception) {
            }
        }
        backgroundPauseRunnable = r
        handler.postDelayed(r, 5000)
    }

    /**
     * Called when Activity returns to foreground.
     */
    fun onResume(webView: WebView?) {
        webView ?: return

        backgroundPauseRunnable?.let {
            handler.removeCallbacks(it)
            backgroundPauseRunnable = null
        }

        webView.resumeTimers()
        webView.onResume()

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
