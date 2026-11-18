package com.izumii.bcandroid

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import android.widget.Toast
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat

/**
 * Custom WebView that intercepts window visibility changes to prevent
 * Chromium Blink renderer from entering the 3-minute background freezing cycle,
 * and bridges IME/keyboard rich image insertion into WebView paste events.
 */
class KeepAliveWebView : WebView {

    constructor(context: Context) : super(context) {
        setupRichContentSupport()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupRichContentSupport()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setupRichContentSupport()
    }

    // Default false saat di foreground agar siklus IME / Keyboard normal 100%
    var keepAliveInBackground = false

    private fun setupRichContentSupport() {
        ViewCompat.setOnReceiveContentListener(
            this,
            ClipboardImageBridge.SUPPORTED_IMAGE_MIMES
        ) { _, payload ->
            handleReceivedContent(payload)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val baseConnection = super.onCreateInputConnection(outAttrs) ?: return null
        if (outAttrs.inputType != InputType.TYPE_NULL) {
            val existing = EditorInfoCompat.getContentMimeTypes(outAttrs)
            val merged = (existing.toList() + ClipboardImageBridge.SUPPORTED_IMAGE_MIMES)
                .distinct()
                .toTypedArray()
            EditorInfoCompat.setContentMimeTypes(outAttrs, merged)
            return InputConnectionCompat.createWrapper(this, baseConnection, outAttrs)
        }
        return baseConnection
    }

    private fun handleReceivedContent(payload: ContentInfoCompat): ContentInfoCompat? {
        val match = ClipboardImageBridge.findImageUriInClip(context, payload.clip) ?: return payload
        val result = ClipboardImageBridge.readImagePayloadFromUri(context, match.first, match.second)
        if (result.tooLarge) {
            Toast.makeText(context, "Image too large to paste (max 12 MB)", Toast.LENGTH_SHORT).show()
            return null
        }
        val payloadJson = result.payloadJson
        if (payloadJson.isNullOrEmpty()) {
            Toast.makeText(context, "Failed to read pasted image", Toast.LENGTH_SHORT).show()
            return null
        }
        ClipboardImageBridge.stashPendingPayload(payloadJson)
        post {
            evaluateJavascript(
                "window._bcConsumePendingImagePaste && window._bcConsumePendingImagePaste();",
                null
            )
        }
        return null
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (keepAliveInBackground && visibility != View.VISIBLE) {
            super.onWindowVisibilityChanged(View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }
}
