package com.izumii.bcandroid

import android.app.Application
import android.content.ComponentCallbacks2
import android.webkit.WebView

class BCApplication : Application(), ComponentCallbacks2 {

    override fun onCreate() {
        super.onCreate()

        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Memory optimizer handles activity-bound webview instances
    }

    override fun onLowMemory() {
        super.onLowMemory()
        System.gc()
    }
}
