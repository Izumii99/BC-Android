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
    private const val GITHUB_REPO = "Izumii99/BC-Android"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun getSavedVersion(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SAVED_VERSION, 132)
    }

    fun saveVersion(context: Context, version: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SAVED_VERSION, version).apply()
    }

    private suspend fun isVersionLive(client: OkHttpClient, version: Int): Boolean = withContext(Dispatchers.IO) {
        val url = "https://www.bondage-asia.com/club/R$version/"
        try {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun detectLatestGameUrl(context: Context): String = withContext(Dispatchers.IO) {
        var activeVersion = getSavedVersion(context)
        if (activeVersion < 132 || activeVersion > 500) {
            activeVersion = 132
        }

        val candidates = mutableSetOf<Int>()
        for (i in 0..8) {
            candidates.add(activeVersion + i)
        }
        for (i in 1..3) {
            if (activeVersion - i >= 132) candidates.add(activeVersion - i)
        }

        // Fast-forward candidate check using GitGud CHANGELOG if available
        try {
            val changelogReq = Request.Builder()
                .url("https://gitgud.io/BondageProjects/Bondage-College/-/raw/master/BondageClub/CHANGELOG.md")
                .header("User-Agent", "BC-Android-Client")
                .build()
            httpClient.newCall(changelogReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        val reader = BufferedReader(StringReader(bodyString))
                        var line: String?
                        var lineCount = 0
                        val pattern = Pattern.compile("^## \\[R(\\d+)\\]")
                        while (reader.readLine().also { line = it } != null && lineCount < 100) {
                            lineCount++
                            val currentLine = line ?: break
                            val matcher = pattern.matcher(currentLine)
                            if (matcher.find()) {
                                val verStr = matcher.group(1)
                                val ver = verStr?.toIntOrNull()
                                if (ver != null && ver in 132..499) {
                                    candidates.add(ver)
                                }
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }

        // Probe candidates concurrently
        val deferredList = candidates.map { ver ->
            async {
                val live = isVersionLive(httpClient, ver)
                Pair(ver, live)
            }
        }

        val results = deferredList.awaitAll()
        val bestLiveVersion = results
            .filter { it.second }
            .map { it.first }
            .maxOrNull()

        if (bestLiveVersion != null && bestLiveVersion > 0) {
            saveVersion(context, bestLiveVersion)
            return@withContext "https://www.bondage-asia.com/club/R$bestLiveVersion/"
        }

        "https://www.bondage-asia.com/club/R$activeVersion/"
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
