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
        "if(!window._bcRequestBgBurst){" +
        "window._bcRequestBgBurst=function(ms){const t=performance.now()+(ms||2500);if(t>(window._bcBgBurstUntil||0))window._bcBgBurstUntil=t;};" +
        "}" +
        "if(!window.__bcDrawHooked&&typeof window.DrawProcess==='function'){" +
        "window.__bcDrawHooked=true;" +
        "const _origDraw=window.DrawProcess;" +
        "let _lastBgDrawTime=0;" +
        "window.DrawProcess=function(...args){" +
        "if(window._bcIsMinimized)return;" +
        "const now=performance.now();" +
        "if(window._bcIsBackground){" +
        "const isBurst=now<(window._bcBgBurstUntil||0);" +
        "if(isBurst?(now-_lastBgDrawTime<66):(now-_lastBgDrawTime<1000))return;" +
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
        "if(window._bcIsMinimized){" +
        "if(C)C._bcPendingCanvasBuild=true;" +
        "return;" +
        "}" +
        "if(window._bcIsBackground&&window._bcRequestBgBurst)window._bcRequestBgBurst(2500);" +
        "if(C&&C!==window.CurrentCharacter&&C.Canvas&&C.CanvasBlink&&window.CurrentScreen==='ChatRoom'){" +
        "const minInterval=window._bcIsBackground?120:(C===window.Player?16:32);" +
        "const now=performance.now();" +
        "const elapsed=now-(C._bcLastCanvasBuild||0);" +
        "if(elapsed<minInterval){" +
        "if(!C._bcBuildTimer){C._bcBuildTimer=setTimeout(()=>{C._bcBuildTimer=null;if(typeof window.CharacterLoadCanvas==='function')window.CharacterLoadCanvas(C);},minInterval-elapsed);}" +
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
        "if(window._bcIsMinimized){" +
        "if(C)C._bcPendingCanvasBuild=true;" +
        "return;" +
        "}" +
        "if(window._bcIsBackground&&window._bcRequestBgBurst)window._bcRequestBgBurst(2500);" +
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
        // --- RAF queue with cancelAnimationFrame support ---
        "if(!window._bcOrigRAF){" +
        "window._bcOrigRAF=window.requestAnimationFrame;" +
        "window._bcOrigCAF=window.cancelAnimationFrame;" +
        "window._bcRafQ=new Map();" +
        "window.requestAnimationFrame=function(cb){const q=window._bcRafQ;const id=-(q.size+1+Math.floor(Math.random()*1e6));const t=setTimeout(function(){q.delete(id);cb(performance.now());},10000);q.set(id,{t:t,cb:cb});return id;};" +
        "window.cancelAnimationFrame=function(id){const q=window._bcRafQ;if(q&&q.has(id)){clearTimeout(q.get(id).t);q.delete(id);}else if(window._bcOrigCAF){window._bcOrigCAF(id);}};" +
        "}" +
        // --- WebGL context loss tracking ---
        "if(!window._bcGlLossTracked&&typeof GLDrawCanvas!=='undefined'&&GLDrawCanvas){" +
        "window._bcGlLossTracked=true;" +
        "GLDrawCanvas.addEventListener('webglcontextlost',function(){window._bcGlWasLost=true;},false);" +
        "GLDrawCanvas.addEventListener('webglcontextrestored',function(){window._bcGlWasLost=true;},false);" +
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
        // --- Flush RAF queue with deduplication + restore cancelAnimationFrame ---
        "if(window._bcOrigRAF){" +
        "const raf=window._bcOrigRAF;" +
        "window.requestAnimationFrame=raf;" +
        "window._bcOrigRAF=null;" +
        "if(window._bcOrigCAF){window.cancelAnimationFrame=window._bcOrigCAF;window._bcOrigCAF=null;}" +
        "const q=window._bcRafQ;window._bcRafQ=null;" +
        "const seen=new Set();" +
        "if(q)q.forEach(function(e){clearTimeout(e.t);if(!seen.has(e.cb)){seen.add(e.cb);raf.call(window,e.cb);}});" +
        "if(typeof window.MainRun==='function'&&!seen.has(window.MainRun)){raf.call(window,window.MainRun);}" +
        "}" +
        // --- Compositor repaint kick (spoof visibility workaround) ---
        "const mc=document.getElementById('MainCanvas');if(mc){mc.style.opacity='0.999';requestAnimationFrame(function(){mc.style.opacity='';});}" +
        // --- WebGL context recovery (300ms) ---
        "setTimeout(function(){try{" +
        "const gl=window.GLDrawContext||(typeof GLDrawCanvas!=='undefined'&&GLDrawCanvas&&(GLDrawCanvas.getContext('webgl2')||GLDrawCanvas.getContext('webgl')));" +
        "if(gl&&typeof gl.isContextLost==='function'&&gl.isContextLost()){" +
        "window._bcGlWasLost=false;" +
        "if(typeof window.GLDrawResetCanvas==='function')window.GLDrawResetCanvas();" +
        "}else if(window._bcGlWasLost){" +
        "window._bcGlWasLost=false;" +
        "if(typeof window.GLDrawResetCanvas==='function'){window.GLDrawResetCanvas();}else{" +
        "const cs2=(Array.isArray(window.ChatRoomCharacter)?window.ChatRoomCharacter.slice():[]);" +
        "if(window.Player&&!cs2.includes(window.Player))cs2.push(window.Player);" +
        "for(let i=0;i<cs2.length;i++){if(cs2[i])cs2[i]._bcPendingCanvasBuild=true;}" +
        "}" +
        "}" +
        "}catch(e){}},300);" +
        // --- Purge delay reset ---
        "const now=typeof window.CommonTime==='function'?window.CommonTime():Date.now();if(window.DrawImageCache)window.DrawImageCache.lastPurge=now;if(window.GLDrawImageCache)window.GLDrawImageCache.lastPurge=now;" +
        // --- Rebuild pending characters ---
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
        // --- Restore hidden DOM ---
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
        if (webView == null) {
            return
        }

        webView.evaluateJavascript(ENTER_UNFOCUSED_SCRIPT, null)
    }

    /**
     * Called when Activity is fully off-screen (e.g. Home pressed, screen off).
     * Stops canvas rendering completely (0 FPS), throttles RAF to 0.25 FPS,
     * and hides canvas & chat DOM for maximum battery savings.
     */
    fun onStop(webView: WebView?) {
        if (webView == null) {
            return
        }

        webView.evaluateJavascript(ENTER_STOPPED_SCRIPT, null)
    }

    /**
     * Called when Activity returns to foreground.
     */
    fun onResume(webView: WebView?) {
        if (webView == null) {
            return
        }

        webView.evaluateJavascript(RESTORE_FOREGROUND_SCRIPT, null)
    }

    /**
     * Reacts to OS memory pressure signals (ComponentCallbacks2).
     */
    fun onTrimMemory(webView: WebView?, level: Int) {
        if (webView == null) {
            return
        }

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
        if (webView == null) {
            return
        }
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
