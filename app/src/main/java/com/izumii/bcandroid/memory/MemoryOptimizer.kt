package com.izumii.bcandroid.memory

import android.content.ComponentCallbacks2
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView

object MemoryOptimizer {

    private const val ENTER_BACKGROUND_SCRIPT =
        "(()=>{" +
        "window._bcIsBackground=true;" +
        "if(!window._bcOrigRAF){" +
        "window._bcOrigRAF=window.requestAnimationFrame;" +
        "window.requestAnimationFrame=function(cb){return setTimeout(function(){cb(performance.now());},1000);};" +
        "}" +
        "if(!window._bcOrigDrawProcess&&typeof window.DrawProcess==='function'){" +
        "window._bcOrigDrawProcess=window.DrawProcess;" +
        "window.DrawProcess=function(...args){if(window._bcIsBackground)return;return window._bcOrigDrawProcess.apply(this,args);};" +
        "}" +
        "if(window.bcFocusMemory&&!window.bcFocusMemory.isConnected)window.bcFocusMemory=null;" +
        "if(window.bcLastMousedown&&!window.bcLastMousedown.isConnected)window.bcLastMousedown=null;" +
        "if(window.bcLastFocusedLayer&&!window.bcLastFocusedLayer.isConnected)window.bcLastFocusedLayer=null;" +
        "if(typeof window.gc==='function'){try{window.gc();}catch(e){}}" +
        "})();"

    private const val RESTORE_FOREGROUND_SCRIPT =
        "(()=>{" +
        "window._bcIsBackground=false;" +
        "if(window._bcOrigRAF){" +
        "window.requestAnimationFrame=window._bcOrigRAF;" +
        "window._bcOrigRAF=null;" +
        "}" +
        "if(window._bcOrigDrawProcess){" +
        "window.DrawProcess=window._bcOrigDrawProcess;" +
        "window._bcOrigDrawProcess=null;" +
        "}" +
        "})();"

    /**
     * Called when Activity is sent to background or loses focus.
     * Throttles graphics engine to 1 FPS and skips canvas rendering without
     * freezing background JS timers / WebSocket connections.
     */
    fun onPause(webView: WebView?) {
        webView ?: return

        webView.evaluateJavascript(ENTER_BACKGROUND_SCRIPT, null)
    }

    /**
     * Called when Activity returns to foreground.
     */
    fun onResume(webView: WebView?) {
        webView ?: return

        webView.evaluateJavascript(RESTORE_FOREGROUND_SCRIPT, null)
    }

    /**
     * Reacts to OS memory pressure signals (ComponentCallbacks2).
     */
    fun onTrimMemory(webView: WebView?, level: Int) {
        webView ?: return

        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // Clear volatile image caches only under severe OS memory pressure
                try {
                    webView.clearCache(false)
                } catch (ignored: Exception) {
                }
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
