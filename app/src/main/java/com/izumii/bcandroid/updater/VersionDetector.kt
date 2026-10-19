package com.izumii.bcandroid.updater

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object VersionDetector {

    const val DEFAULT_FALLBACK_URL = "https://www.bondage-asia.com/club/R132/"
    private const val PREFS_NAME = "BCVersionPrefs"
    private const val KEY_SAVED_VERSION = "saved_version"
    private const val KEY_VERSION_LOCKED = "is_version_locked"
    private const val GITHUB_REPO = "Izumii99/BC-Android"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun readLocalVersionFile(context: Context): Pair<Int, Boolean>? {
        return try {
            val externalFile = context.getExternalFilesDir(null)?.resolve("version.txt")
            val internalFile = java.io.File(context.filesDir, "version.txt")
            val target = when {
                externalFile != null && externalFile.exists() && externalFile.isFile -> externalFile
                internalFile.exists() && internalFile.isFile -> internalFile
                else -> null
            }
            target?.readText()?.let { parseVersionConfig(it) }
        } catch (ignored: Exception) {
            null
        }
    }

    fun getSavedVersion(context: Context): Int {
        val fileConfig = readLocalVersionFile(context)
        if (fileConfig != null) return fileConfig.first

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SAVED_VERSION, 132)
    }

    fun isVersionLocked(context: Context): Boolean {
        val fileConfig = readLocalVersionFile(context)
        if (fileConfig != null) return fileConfig.second

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VERSION_LOCKED, false)
    }

    fun saveVersion(context: Context, version: Int, locked: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_SAVED_VERSION, version)
            .putBoolean(KEY_VERSION_LOCKED, locked)
            .apply()
    }

    fun setVersionLocked(context: Context, locked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VERSION_LOCKED, locked).apply()
    }

    fun parseVersionConfig(raw: String): Pair<Int, Boolean> {
        val trimmed = raw.trim()
        val isLocked = trimmed.endsWith(":lock", ignoreCase = true)
        val verStr = trimmed.substringBefore(":")
        val verNum = verStr.toIntOrNull() ?: 132
        return Pair(verNum, isLocked)
    }

    /**
     * Determines whether the given URL is an active Bondage Club game canvas.
     */
    fun isGameUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val cleanUrl = url.trim().lowercase()

        val isBcHost = cleanUrl.contains("bondage-asia.com") ||
                       cleanUrl.contains("bondageprojects.com") ||
                       cleanUrl.contains("bondageprojects.elementfx.com") ||
                       cleanUrl.contains("bondage-europe.com")

        if (!isBcHost) return false

        return cleanUrl.contains("/club") || cleanUrl.contains("/r1") || cleanUrl.contains("/r2")
    }

    enum class VersionStatus {
        NotFound,
        Active,
        Outdated
    }

    fun parseNextVersionFromHtml(html: String, fallbackVersion: Int): Pair<VersionStatus, Int?> {
        val matcher = Pattern.compile("URL=(?:\\.\\./)?R(\\d+)/", Pattern.CASE_INSENSITIVE).matcher(html)
        if (matcher.find()) {
            val nextVer = matcher.group(1)?.toIntOrNull()
            if (nextVer != null) {
                return Pair(VersionStatus.Outdated, nextVer)
            }
        }

        if (html.contains("Outdated Version", ignoreCase = true)) {
            return Pair(VersionStatus.Outdated, fallbackVersion + 1)
        }

        return Pair(VersionStatus.Active, null)
    }

    suspend fun checkVersionStatus(client: OkHttpClient, version: Int): Pair<VersionStatus, Int?> = withContext(Dispatchers.IO) {
        val url = "https://www.bondage-asia.com/club/R$version/"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext Pair(VersionStatus.NotFound, null)
                val html = response.body?.string() ?: ""
                parseNextVersionFromHtml(html, version)
            }
        } catch (e: Exception) {
            Pair(VersionStatus.NotFound, null)
        }
    }

    suspend fun detectLatestGameUrl(context: Context): String = withContext(Dispatchers.IO) {
        val fileConfig = readLocalVersionFile(context)
        val activeVersion = fileConfig?.first ?: getSavedVersion(context)
        val isLocked = fileConfig?.second ?: isVersionLocked(context)

        if (isLocked) {
            return@withContext "https://www.bondage-asia.com/club/R$activeVersion/"
        }

        var candidate = if (activeVersion in 132..500) activeVersion else 132
        var currentProbe = candidate
        val maxHops = 10

        for (hop in 0 until maxHops) {
            val (status, nextVer) = checkVersionStatus(httpClient, currentProbe)
            if (status == VersionStatus.Active) {
                candidate = currentProbe
                break
            } else if (status == VersionStatus.Outdated && nextVer != null) {
                val (nextStatus, _) = checkVersionStatus(httpClient, nextVer)
                if (nextStatus != VersionStatus.NotFound) {
                    candidate = nextVer
                    currentProbe = nextVer
                    if (nextStatus == VersionStatus.Active) break
                } else {
                    break
                }
            } else {
                if (currentProbe > 132) {
                    currentProbe--
                    continue
                }
                break
            }
        }

        if (fileConfig == null) {
            saveVersion(context, candidate, false)
        }
        "https://www.bondage-asia.com/club/R$candidate/"
    }

    /**
     * Checks if GitHub release tag is strictly newer than current app version.
     */
    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        if (latestTag.isBlank() || currentVersion.isBlank()) return false

        val cleanLatest = latestTag.trim().trimStart('v', 'V')
        val cleanCurrent = currentVersion.trim().trimStart('v', 'V')

        val latestParts = cleanLatest.split('.').map { it.toIntOrNull() ?: 0 }
        val currentParts = cleanCurrent.split('.').map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    data class AppReleaseInfo(val hasUpdate: Boolean, val latestTag: String, val downloadUrl: String)

    suspend fun checkForAppUpdate(currentVersion: String): AppReleaseInfo = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .header("User-Agent", "BC-Android-Client")
                .build()

            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext AppReleaseInfo(false, "", "")
                val jsonStr = response.body?.string() ?: return@withContext AppReleaseInfo(false, "", "")
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases/latest")

                if (tagName.isNotEmpty() && isNewerVersion(tagName, currentVersion)) {
                    return@withContext AppReleaseInfo(true, tagName, htmlUrl)
                }
            }
        } catch (ignored: Exception) {
        }
        AppReleaseInfo(false, "", "")
    }
}
