package com.izumii.bcandroid

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

/**
 * Custom WebView that intercepts window visibility changes to prevent
 * Chromium Blink renderer from entering the 3-minute background freezing cycle.
 */
class KeepAliveWebView : WebView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var keepAliveInBackground = true

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (keepAliveInBackground && visibility != View.VISIBLE) {
            super.onWindowVisibilityChanged(View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }
}
