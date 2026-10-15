package com.izumii.bcandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun copyText(text: String) {
        mainHandler.post {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Bondage Club Text", text)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) {
            }
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}
