@file:Suppress("TooGenericExceptionCaught")

package com.izumii.bcandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

/**
 * Validates and converts Android content:// clipboard and IME images into safe
 * base64 JSON payloads for WebView ClipboardEvent synthesis.
 */
object ClipboardImageBridge {

    const val MAX_IMAGE_BYTES = 12 * 1024 * 1024

    val SUPPORTED_IMAGE_MIMES = arrayOf(
        "image/png",
        "image/jpeg",
        "image/webp",
        "image/gif",
        "image/bmp",
        "image/*"
    )

    private val ALLOWED_EXACT_MIMES = setOf(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/webp",
        "image/gif",
        "image/bmp"
    )

    private val pendingPastedPayload = AtomicReference<String?>(null)

    fun stashPendingPayload(payloadJson: String) {
        pendingPastedPayload.set(payloadJson)
    }

    fun consumePendingPayload(): String {
        return pendingPastedPayload.getAndSet(null) ?: ""
    }

    fun findClipboardImageUri(context: Context): Pair<Uri, String?>? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        if (!clipboard.hasPrimaryClip()) {
            return null
        }
        val clip = clipboard.primaryClip ?: return null
        return findImageUriInClip(context, clip)
    }

    fun findImageUriInClip(context: Context, clip: ClipData): Pair<Uri, String?>? {
        val desc = clip.description
        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i)?.uri ?: continue
            if (!uri.scheme.equals("content", ignoreCase = true)) {
                continue
            }
            val resolverMime = try {
                context.contentResolver.getType(uri)
            } catch (ignored: Exception) {
                null
            }
            val descMime = if (i == 0 && desc != null && desc.mimeTypeCount > 0) {
                (0 until desc.mimeTypeCount)
                    .map { idx -> desc.getMimeType(idx) }
                    .firstOrNull { it.startsWith("image/", ignoreCase = true) }
            } else {
                null
            }
            val hint = resolverMime ?: descMime
            if (hint != null && hint.startsWith("image/", ignoreCase = true)) {
                return uri to hint
            }
        }
        return null
    }

    data class ReadResult(
        val payloadJson: String?,
        val tooLarge: Boolean = false
    )

    fun readImagePayloadFromUri(
        context: Context,
        uri: Uri,
        mimeHint: String? = null
    ): ReadResult {
        if (!uri.scheme.equals("content", ignoreCase = true)) {
            return ReadResult(null)
        }
        return try {
            val resolvedHint = mimeHint ?: context.contentResolver.getType(uri)
            val streamResult = context.contentResolver.openInputStream(uri)?.use { stream ->
                readBoundedBytes(stream, MAX_IMAGE_BYTES)
            } ?: return ReadResult(null)

            if (streamResult.tooLarge) {
                return ReadResult(null, tooLarge = true)
            }
            val bytes = streamResult.bytes ?: return ReadResult(null)
            if (bytes.isEmpty()) {
                return ReadResult(null)
            }
            val mime = normalizeImageMime(resolvedHint, bytes) ?: return ReadResult(null)
            ReadResult(buildPayloadJson(mime, bytes))
        } catch (e: Exception) {
            ReadResult(null)
        }
    }

    data class BoundedBytes(
        val bytes: ByteArray?,
        val tooLarge: Boolean = false
    )

    fun readBoundedBytes(input: InputStream, maxBytes: Int = MAX_IMAGE_BYTES): BoundedBytes {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buf)
            if (read == -1) {
                break
            }
            total += read
            if (total > maxBytes) {
                return BoundedBytes(null, tooLarge = true)
            }
            out.write(buf, 0, read)
        }
        return BoundedBytes(out.toByteArray(), tooLarge = false)
    }

    fun normalizeImageMime(rawMime: String?, headerBytes: ByteArray?): String? {
        val cleaned = rawMime?.substringBefore(';')?.trim()?.lowercase()
        val normalized = if (cleaned == "image/jpg") "image/jpeg" else cleaned
        if (normalized != null && normalized in ALLOWED_EXACT_MIMES) {
            return normalized
        }
        if (normalized == null || normalized == "image/*" || normalized == "application/octet-stream") {
            return sniffImageMime(headerBytes)
        }
        return null
    }

    fun sniffImageMime(bytes: ByteArray?): String? {
        if (bytes == null || bytes.size < 4) {
            return null
        }
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF

        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
            return "image/png"
        }
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return "image/jpeg"
        }
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) {
            return "image/gif"
        }
        if (b0 == 0x42 && b1 == 0x4D) {
            return "image/bmp"
        }
        if (bytes.size >= 12 &&
            b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 &&
            (bytes[8].toInt() and 0xFF) == 0x57 &&
            (bytes[9].toInt() and 0xFF) == 0x45 &&
            (bytes[10].toInt() and 0xFF) == 0x42 &&
            (bytes[11].toInt() and 0xFF) == 0x50
        ) {
            return "image/webp"
        }
        return null
    }

    fun extensionForMime(mime: String): String {
        return when (mime.lowercase()) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/bmp" -> "bmp"
            else -> "png"
        }
    }

    fun buildPayloadJson(mime: String, bytes: ByteArray): String {
        val ext = extensionForMime(mime)
        val b64 = Base64.getEncoder().encodeToString(bytes)
        return JSONObject().apply {
            put("dataUrl", "data:$mime;base64,$b64")
            put("mime", mime)
            put("name", "pasted_${System.currentTimeMillis()}.$ext")
        }.toString()
    }
}
