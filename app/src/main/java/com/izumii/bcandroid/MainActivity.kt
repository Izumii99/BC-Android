package com.izumii.bcandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.izumii.bcandroid.memory.MemoryOptimizer
import com.izumii.bcandroid.scripts.ScriptLoader
import com.izumii.bcandroid.service.ConnectionKeeperService
import com.izumii.bcandroid.updater.VersionDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private var webView: WebView? = null
    private var pendingInstallFile: File? = null

    private var backPressedTime: Long = 0
    private var recentCrashCount = 0
    private var lastCrashTime = 0L

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    private var currentLoadedUrl: String = VersionDetector.DEFAULT_FALLBACK_URL
    private var isAppInForeground = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootContainer = findViewById(R.id.rootContainer)

        // Request runtime notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setupImmersiveFullscreen()
        setupDoubleTapBackToExit()
        setupWebView()

        // Clean any leftover APK update files from previous sessions
        VersionDetector.cleanCachedApks(this)

        // Asynchronously probe and detect the newest game release URL
        lifecycleScope.launch {
            val targetUrl = VersionDetector.detectLatestGameUrl(this@MainActivity)
            currentLoadedUrl = targetUrl
            webView?.loadUrl(targetUrl)

            // Interactive update checker for newer APK releases
            val updateInfo = VersionDetector.checkForAppUpdate(BuildConfig.VERSION_NAME, BuildConfig.FLAVOR)
            if (updateInfo.hasUpdate) {
                showUpdateDialog(updateInfo)
            }
        }
    }

    private fun setupImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun setupDoubleTapBackToExit() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val wv = webView
                val currentUrl = wv?.url

                // If currently viewing an external or non-game link, and can navigate back in history:
                if (wv != null && wv.canGoBack() && !VersionDetector.isGameUrl(currentUrl)) {
                    wv.goBack()
                    return
                }

                // If already at the main game canvas or no previous page exists, require double-tap to exit
                val now = System.currentTimeMillis()
                if (now - backPressedTime < 2000) {
                    finish()
                } else {
                    backPressedTime = now
                    Toast.makeText(this@MainActivity, R.string.exit_prompt, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun openExternalChooser(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, getString(R.string.open_with)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val existingView = findViewById<KeepAliveWebView>(R.id.webView)
        val v = existingView ?: KeepAliveWebView(this).also {
            rootContainer.addView(
                it,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        webView = v

        v.setBackgroundColor(Color.parseColor("#0D0D0D"))
        v.isFocusable = true
        v.isFocusableInTouchMode = true
        v.isLongClickable = true
        v.isHapticFeedbackEnabled = true

        val settings = v.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = true

        val bridge = WebAppInterface(
            context = this,
            onOfflineAddonsRequested = {
                ScriptLoader.injectLocalAddons(this@MainActivity, webView)
            },
            onZoomStateChanged = { enabled ->
                webView?.let { wv ->
                    wv.settings.setSupportZoom(enabled)
                    wv.settings.builtInZoomControls = enabled
                    if (!enabled) {
                        wv.scrollTo(0, 0)
                    }
                }
            }
        )
        v.addJavascriptInterface(bridge, "AndroidBridge")

        v.setDownloadListener { url, _, contentDisposition, mimetype, _ ->
            val guessedName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
            if (url.startsWith("data:")) {
                bridge.saveBase64File(url, guessedName, mimetype)
            } else if (url.startsWith("blob:")) {
                val js = """
                    (function() {
                        fetch('$url')
                            .then(function(r) { return r.blob(); })
                            .then(function(b) {
                                const reader = new FileReader();
                                reader.onloadend = function() {
                                    if (window.AndroidBridge && typeof window.AndroidBridge.saveBase64File === 'function') {
                                        window.AndroidBridge.saveBase64File(reader.result, '$guessedName', b.type || '$mimetype');
                                    }
                                };
                                reader.readAsDataURL(b);
                            }).catch(function() {});
                    })();
                """.trimIndent()
                v.evaluateJavascript(js, null)
            } else {
                bridge.downloadHttpFile(url, guessedName, mimetype)
            }
        }

        v.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = try {
                    fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                } catch (e: Exception) {
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                }

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = null
                    false
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!isAppInForeground) {
                    val count = title?.let {
                        val match = Regex("""^\((\d+)\)""").find(it)
                        match?.groupValues?.get(1)?.toIntOrNull()
                    } ?: 0
                    ConnectionKeeperService.updateBadgeCount(this@MainActivity, count)
                }
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                if (isFinishing || isDestroyed) return false
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setMessage(message ?: "")
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                if (isFinishing || isDestroyed) return false
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setMessage(message ?: "")
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: android.webkit.JsPromptResult?
            ): Boolean {
                if (isFinishing || isDestroyed) return false
                val input = android.widget.EditText(this@MainActivity).apply {
                    setText(defaultValue ?: "")
                    setSelection(text.length)
                }
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setMessage(message ?: "")
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        result?.confirm(input.text.toString())
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result?.cancel()
                    }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
        }

        v.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (VersionDetector.isGameUrl(url)) {
                    return false
                }
                return openExternalChooser(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (VersionDetector.isGameUrl(url)) {
                    ScriptLoader.injectCoreScripts(this@MainActivity, view)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                // Auto-fallback if the probed URL 404s
                if (request?.isForMainFrame == true && errorResponse?.statusCode == 404) {
                    if (currentLoadedUrl != VersionDetector.DEFAULT_FALLBACK_URL) {
                        currentLoadedUrl = VersionDetector.DEFAULT_FALLBACK_URL
                        view?.loadUrl(VersionDetector.DEFAULT_FALLBACK_URL)
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
            }

            /**
             * Crash auto-recovery with crash-loop protection (mirrors desktop client)
             */
            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                val now = System.currentTimeMillis()
                if (now - lastCrashTime < 30_000) {
                    recentCrashCount++
                } else {
                    recentCrashCount = 1
                }
                lastCrashTime = now

                Toast.makeText(this@MainActivity, R.string.reloading_game, Toast.LENGTH_SHORT).show()

                // Remove zombie webview and recreate cleanly
                MemoryOptimizer.destroyWebView(webView)
                webView = null

                if (recentCrashCount <= 3) {
                    setupWebView()
                    webView?.loadUrl(currentLoadedUrl)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Excessive crashes detected. Reverting to base version.",
                        Toast.LENGTH_LONG
                    ).show()
                    currentLoadedUrl = VersionDetector.DEFAULT_FALLBACK_URL
                    setupWebView()
                    webView?.loadUrl(VersionDetector.DEFAULT_FALLBACK_URL)
                }
                return true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        (webView as? KeepAliveWebView)?.keepAliveInBackground = false
        setupImmersiveFullscreen()
        MemoryOptimizer.onResume(webView)
        ConnectionKeeperService.onPulseListener = null
        ConnectionKeeperService.stop(this)
        ConnectionKeeperService.updateBadgeCount(this, 0)

        // Check if an update was downloaded and waiting for install permission
        val pending = pendingInstallFile
        if (pending != null && pending.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()) {
                pendingInstallFile = null
                triggerApkInstall(pending)
            }
        }

        // Seamless resume check: auto-restore if socket disconnected or dropped while backgrounded
        webView?.evaluateJavascript(
            "(()=>{try{if(window.CurrentScreen==='Relog'&&typeof window.RelogSend==='function'){window.RelogSend();}else if(window.ServerSocket&&!window.ServerSocket.connected&&typeof window.ServerInit==='function'){window.ServerInit();}}catch(e){}})();",
            null
        )
    }

    override fun onPause() {
        isAppInForeground = false
        (webView as? KeepAliveWebView)?.keepAliveInBackground = true
        super.onPause()
        MemoryOptimizer.onPause(webView)
        ConnectionKeeperService.onPulseListener = {
            webView?.evaluateJavascript(
                """
                (()=>{
                    try {
                        if (window.ServerSocket) {
                            if (window.ServerSocket.connected) {
                                window.ServerSocket.emit('Ping');
                                if (window.ServerSocket.io && window.ServerSocket.io.engine) {
                                    window.ServerSocket.io.engine.write('2');
                                }
                            } else if (typeof window.ServerInit === 'function') {
                                window.ServerInit();
                            }
                        }
                        if (window.CurrentScreen === 'Relog' && typeof window.RelogSend === 'function') {
                            window.RelogSend();
                        }
                    } catch(e) {}
                })();
                """.trimIndent(),
                null
            )
        }
        ConnectionKeeperService.start(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryOptimizer.onTrimMemory(webView, level)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveFullscreen()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        setupImmersiveFullscreen()
    }

    override fun onDestroy() {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = null
        ConnectionKeeperService.onPulseListener = null
        ConnectionKeeperService.stop(this)
        MemoryOptimizer.destroyWebView(webView)
        webView = null
        super.onDestroy()
    }

    private fun showUpdateDialog(updateInfo: VersionDetector.AppReleaseInfo) {
        val hasDirectApk = !updateInfo.apkDownloadUrl.isNullOrEmpty()
        val message = if (hasDirectApk) {
            "A new version of BC Android (${updateInfo.latestTag}) is available. Would you like to download and install it now?"
        } else {
            "A new version of BC Android (${updateInfo.latestTag}) is available. Would you like to view the release page?"
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Update Available (${updateInfo.latestTag})")
            .setMessage(message)
            .setPositiveButton(if (hasDirectApk) "Update Now" else "Download") { _, _ ->
                val apkUrl = updateInfo.apkDownloadUrl
                if (!apkUrl.isNullOrEmpty()) {
                    startInstantUpdate(updateInfo)
                } else {
                    openBrowserUrl(updateInfo.downloadUrl)
                }
            }
            .setNegativeButton("Later", null)

        if (hasDirectApk) {
            builder.setNeutralButton("Notes") { _, _ ->
                openBrowserUrl(updateInfo.downloadUrl)
            }
        }

        builder.show()
    }

    private fun startInstantUpdate(updateInfo: VersionDetector.AppReleaseInfo) {
        val apkUrl = updateInfo.apkDownloadUrl ?: return

        val progressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            setPadding(60, 30, 60, 30)
        }

        var downloadJob: kotlinx.coroutines.Job? = null

        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Downloading Update")
            .setMessage("Downloading BC Android ${updateInfo.latestTag}...")
            .setView(progressBar)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ ->
                downloadJob?.cancel()
            }
            .create()

        progressDialog.show()

        downloadJob = lifecycleScope.launch {
            val apkFile = VersionDetector.downloadApk(this@MainActivity, apkUrl) { percent ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.progress = percent
                }
            }

            progressDialog.dismiss()

            if (apkFile != null && apkFile.exists()) {
                triggerApkInstall(apkFile)
            } else if (isActive) {
                Toast.makeText(
                    this@MainActivity,
                    "Download failed. Opening release page...",
                    Toast.LENGTH_SHORT
                ).show()
                openBrowserUrl(updateInfo.downloadUrl)
            }
        }
    }

    private fun triggerApkInstall(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                pendingInstallFile = apkFile
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("To install the update, please allow 'Install unknown apps' for BC Android in settings.")
                    .setPositiveButton("Settings") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }
        }

        try {
            val intent = VersionDetector.createInstallIntent(this, apkFile)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch installer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBrowserUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ignored: Exception) {
        }
    }
}
