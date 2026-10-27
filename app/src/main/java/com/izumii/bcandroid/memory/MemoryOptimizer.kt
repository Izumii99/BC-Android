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
        "window.dispatchEvent(new CustomEvent('bc:visibility',{detail:{background:true}}));" +
        "if(!window.__bcFocusOverridden){" +
        "window.__bcFocusOverridden=true;" +
        "const _origHasFocus=document.hasFocus.bind(document);" +
        "document.hasFocus=function(){return !window._bcIsBackground&&_origHasFocus();};" +
        "}" +
        "if(!window._bcOrigRAF){" +
        "window._bcOrigRAF=window.requestAnimationFrame;" +
        "window.requestAnimationFrame=function(cb){return setTimeout(function(){cb(performance.now());},4000);};" +
        "}" +
        "if(!window.__bcDrawHooked&&typeof window.DrawProcess==='function'){" +
        "window.__bcDrawHooked=true;" +
        "const _wrappedDraw=window.DrawProcess;" +
        "window.DrawProcess=function(...args){if(window._bcIsBackground)return;return _wrappedDraw.apply(this,args);};" +
        "}" +
        "const canvas=document.getElementById('MainCanvas');if(canvas)canvas.style.visibility='hidden';" +
        "const chatLog=document.getElementById('TextAreaChatLog');if(chatLog)chatLog.style.contentVisibility='hidden';" +
        "const paws=document.getElementById('bcn-soft-paws');if(paws)paws.style.display='none';" +
        "if(window.bcFocusMemory&&!window.bcFocusMemory.isConnected)window.bcFocusMemory=null;" +
        "if(window.bcLastMousedown&&!window.bcLastMousedown.isConnected)window.bcLastMousedown=null;" +
        "if(window.bcLastFocusedLayer&&!window.bcLastFocusedLayer.isConnected)window.bcLastFocusedLayer=null;" +
        "if(typeof window.gc==='function'){try{const now=performance.now();if(!window._bcLastGc||now-window._bcLastGc>60000){window._bcLastGc=now;window.gc();}}catch(e){}}" +
        "})();"

    private const val RESTORE_FOREGROUND_SCRIPT =
        "(()=>{" +
        "window._bcIsBackground=false;" +
        "window.dispatchEvent(new CustomEvent('bc:visibility',{detail:{background:false}}));" +
        "if(window._bcOrigRAF){" +
        "window.requestAnimationFrame=window._bcOrigRAF;" +
        "window._bcOrigRAF=null;" +
        "}" +
        "const canvas=document.getElementById('MainCanvas');if(canvas)canvas.style.visibility='';" +
        "const chatLog=document.getElementById('TextAreaChatLog');if(chatLog)chatLog.style.contentVisibility='';" +
        "const paws=document.getElementById('bcn-soft-paws');if(paws)paws.style.display='';" +
        "})();"

    /**
     * Called when Activity is sent to background or loses focus.
     * Throttles graphics engine to 0.25 FPS (4s), skips canvas rendering, and freezes DOM layout
     * without freezing background JS timers / WebSocket connections.
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
