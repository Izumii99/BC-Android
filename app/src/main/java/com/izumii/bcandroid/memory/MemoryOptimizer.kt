package com.izumii.bcandroid.memory

import android.content.ComponentCallbacks2
import android.view.ViewGroup
import android.webkit.WebView

object MemoryOptimizer {

    private const val ENTER_UNFOCUSED_SCRIPT =
        "(()=>{" +
        "window._bcIsBackground=true;" +
        "window._bcIsMinimized=false;" +
        "window.dispatchEvent(new CustomEvent('bc:visibility',{detail:{background:true,minimized:false}}));" +
        "if(!window.__bcFocusOverridden){" +
        "window.__bcFocusOverridden=true;" +
        "const _origHasFocus=document.hasFocus.bind(document);" +
        "document.hasFocus=function(){return !window._bcIsBackground&&_origHasFocus();};" +
        "}" +
        "const _attachDrawHook=()=>{" +
        "if(!window.__bcDrawHooked&&typeof window.DrawProcess==='function'){" +
        "window.__bcDrawHooked=true;" +
        "const _origDraw=window.DrawProcess;" +
        "let _lastBgDrawTime=0;" +
        "window.DrawProcess=function(...args){" +
        "if(window._bcIsMinimized)return;" +
        "const now=performance.now();" +
        "if(window._bcIsBackground){" +
        "if(now-_lastBgDrawTime<66)return;" +
        "_lastBgDrawTime=now;" +
        "}else{" +
        "const idle=(now-(window._bcLastInputTime||0))>2500&&!window.CurrentCharacter&&!(typeof window.StruggleProgress==='number'&&window.StruggleProgress>=0);" +
        "if(now-(window._bcLastFgDrawTime||0)<(idle?66:32))return;" +
        "window._bcLastFgDrawTime=now;" +
        "}" +
        "return _origDraw.apply(this,args);" +
        "};" +
        "}" +
        "if(!window.__bcScrollHooked&&typeof window.ElementScrollToEnd==='function'){" +
        "window.__bcScrollHooked=true;" +
        "const _origScroll=window.ElementScrollToEnd;" +
        "window.ElementScrollToEnd=function(id){" +
        "if(window._bcIsMinimized){window._bcNeedsChatScroll=true;return;}" +
        "return _origScroll.apply(this,arguments);" +
        "};" +
        "}" +
        "if(!window.__bcLoadCanvasHooked&&typeof window.CharacterLoadCanvas==='function'){" +
        "window.__bcLoadCanvasHooked=true;" +
        "const _origLoad=window.CharacterLoadCanvas;" +
        "window.CharacterLoadCanvas=function(C){" +
        "if(window._bcIsMinimized||window._bcIsBackground){" +
        "if(C)C._bcPendingCanvasBuild=true;" +
        "return;" +
        "}" +
        "if(C&&C!==window.Player&&C!==window.CurrentCharacter&&C.Canvas&&C.CanvasBlink&&window.CurrentScreen==='ChatRoom'){" +
        "const now=performance.now();" +
        "const elapsed=now-(C._bcLastCanvasBuild||0);" +
        "if(elapsed<150){" +
        "if(!C._bcBuildTimer){C._bcBuildTimer=setTimeout(()=>{C._bcBuildTimer=null;if(typeof window.CharacterLoadCanvas==='function')window.CharacterLoadCanvas(C);},150-elapsed);}" +
        "return;" +
        "}" +
        "C._bcLastCanvasBuild=now;" +
        "}" +
        "return _origLoad.apply(this,arguments);" +
        "};" +
        "}" +
        "if(!window.__bcBuildCanvasHooked&&typeof window.CharacterAppearanceBuildCanvas==='function'){" +
        "window.__bcBuildCanvasHooked=true;" +
        "const _origBuild=window.CharacterAppearanceBuildCanvas;" +
        "window.CharacterAppearanceBuildCanvas=function(C){" +
        "if(window._bcIsMinimized||window._bcIsBackground){" +
        "if(C)C._bcPendingCanvasBuild=true;" +
        "return;" +
        "}" +
        "return _origBuild.apply(this,arguments);" +
        "};" +
        "}" +
        "return Boolean(window.__bcDrawHooked&&window.__bcLoadCanvasHooked&&window.__bcBuildCanvasHooked);" +
        "};" +
        "if(!_attachDrawHook()&&!window._bcDrawHookTimer){" +
        "window._bcDrawHookTimer=setInterval(()=>{" +
        "if(_attachDrawHook()){clearInterval(window._bcDrawHookTimer);window._bcDrawHookTimer=null;}" +
        "},200);" +
        "}" +
        "if(window.bcFocusMemory&&!window.bcFocusMemory.isConnected)window.bcFocusMemory=null;" +
        "if(window.bcLastMousedown&&!window.bcLastMousedown.isConnected)window.bcLastMousedown=null;" +
        "if(window.bcLastFocusedLayer&&!window.bcLastFocusedLayer.isConnected)window.bcLastFocusedLayer=null;" +
        "if(typeof window.gc==='function'){try{const now=performance.now();if(!window._bcLastGc||now-window._bcLastGc>60000){window._bcLastGc=now;window.gc();}}catch(e){}}" +
        "})();"

    private const val ENTER_STOPPED_SCRIPT =
        "(()=>{" +
        "window._bcIsBackground=true;" +
        "window._bcIsMinimized=true;" +
        "window.dispatchEvent(new CustomEvent('bc:visibility',{detail:{background:true,minimized:true}}));" +
        "if(!window._bcOrigRAF){" +
        "window._bcOrigRAF=window.requestAnimationFrame;" +
        "window.requestAnimationFrame=function(cb){return setTimeout(function(){cb(performance.now());},10000);};" +
        "}" +
        "const canvas=document.getElementById('MainCanvas');if(canvas)canvas.style.visibility='hidden';" +
        "const chatLog=document.getElementById('TextAreaChatLog');if(chatLog)chatLog.style.contentVisibility='hidden';" +
        "const paws=document.getElementById('bcn-soft-paws');if(paws)paws.style.display='none';" +
        "})();"

    private const val RESTORE_FOREGROUND_SCRIPT =
        "(()=>{" +
        "window._bcIsBackground=false;" +
        "window._bcIsMinimized=false;" +
        "window._bcLastInputTime=performance.now();" +
        "window._bcLastFgDrawTime=0;" +
        "window.dispatchEvent(new CustomEvent('bc:visibility',{detail:{background:false,minimized:false}}));" +
        "if(window._bcOrigRAF){" +
        "window.requestAnimationFrame=window._bcOrigRAF;" +
        "window._bcOrigRAF=null;" +
        "}" +
        "const now=typeof window.CommonTime==='function'?window.CommonTime():Date.now();if(window.DrawImageCache)window.DrawImageCache.lastPurge=now;if(window.GLDrawImageCache)window.GLDrawImageCache.lastPurge=now;" +
        "const chars=(Array.isArray(window.ChatRoomCharacter)?window.ChatRoomCharacter.slice():[]);" +
        "if(window.Player&&!chars.includes(window.Player))chars.push(window.Player);" +
        "for(let i=0;i<chars.length;i++){" +
        "const c=chars[i];" +
        "if(c&&c._bcPendingCanvasBuild){" +
        "delete c._bcPendingCanvasBuild;" +
        "if(typeof window.CharacterLoadCanvas==='function'){" +
        "try{window.CharacterLoadCanvas(c);}catch(e){}" +
        "}else if(typeof window.CharacterAppearanceBuildCanvas==='function'){" +
        "try{window.CharacterAppearanceBuildCanvas(c);}catch(e){}" +
        "}" +
        "}" +
        "}" +
        "const canvas=document.getElementById('MainCanvas');if(canvas)canvas.style.visibility='';" +
        "const chatLog=document.getElementById('TextAreaChatLog');if(chatLog)chatLog.style.contentVisibility='';" +
        "const paws=document.getElementById('bcn-soft-paws');if(paws)paws.style.display='';" +
        "if(window._bcNeedsChatScroll){window._bcNeedsChatScroll=false;if(typeof window.ElementScrollToEnd==='function'){try{window.ElementScrollToEnd('TextAreaChatLog');}catch(e){}}}" +
        "})();"

    /**
     * Called when Activity loses focus (e.g. Split-Screen / unfocused).
     * Throttles canvas rendering to ~15 FPS without hiding canvas or chat DOM,
     * allowing the user to view both split windows without visual blackout.
     */
    fun onPause(webView: WebView?) {
        webView ?: return

        webView.evaluateJavascript(ENTER_UNFOCUSED_SCRIPT, null)
    }

    /**
     * Called when Activity is fully off-screen (e.g. Home pressed, screen off).
     * Stops canvas rendering completely (0 FPS), throttles RAF to 0.25 FPS,
     * and hides canvas & chat DOM for maximum battery savings.
     */
    fun onStop(webView: WebView?) {
        webView ?: return

        webView.evaluateJavascript(ENTER_STOPPED_SCRIPT, null)
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
