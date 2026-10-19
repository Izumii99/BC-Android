package com.izumii.bcandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(
    private val context: Context,
    private val onOfflineAddonsRequested: (() -> Unit)? = null,
    private val onZoomStateChanged: ((Boolean) -> Unit)? = null
) {

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
    fun loadOfflineAddons() {
        mainHandler.post {
            onOfflineAddonsRequested?.invoke()
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun setZoomEnabled(enabled: Boolean) {
        mainHandler.post {
            onZoomStateChanged?.invoke(enabled)
        }
    }

    @JavascriptInterface
    fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    @JavascriptInterface
    fun openExternalUrl(url: String) {
        mainHandler.post {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, context.getString(R.string.open_with)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (ignored: Exception) {
            }
        }
    }
}
