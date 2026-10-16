package com.izumii.bcandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private var webView: WebView? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootContainer = findViewById(R.id.rootContainer)

        setupImmersiveFullscreen()
        setupDoubleTapBackToExit()
        setupWebView()

        // Asynchronously probe and detect the newest game release URL
        lifecycleScope.launch {
            val targetUrl = VersionDetector.detectLatestGameUrl(this@MainActivity)
            currentLoadedUrl = targetUrl
            webView?.loadUrl(targetUrl)

            // Silent update checker for newer APK releases
            val updateInfo = VersionDetector.checkForAppUpdate(BuildConfig.VERSION_NAME)
            if (updateInfo.hasUpdate) {
                Toast.makeText(
                    this@MainActivity,
                    "New BC Android update available: ${updateInfo.latestTag}",
                    Toast.LENGTH_LONG
                ).show()
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val existingView = findViewById<WebView>(R.id.webView)
        val v = existingView ?: WebView(this).also {
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

        v.addJavascriptInterface(WebAppInterface(
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
        ), "AndroidBridge")

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
        }

        v.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                ScriptLoader.injectCoreScripts(this@MainActivity, view)
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
        setupImmersiveFullscreen()
        MemoryOptimizer.onResume(webView)
        ConnectionKeeperService.onPulseListener = null
        ConnectionKeeperService.stop(this)

        // Seamless resume check: auto-restore if socket disconnected or dropped while backgrounded
        webView?.evaluateJavascript(
            "(()=>{try{if(window.CurrentScreen==='Relog'&&typeof window.RelogSend==='function'){window.RelogSend();}else if(window.ServerSocket&&!window.ServerSocket.connected&&typeof window.ServerInit==='function'){window.ServerInit();}}catch(e){}})();",
            null
        )
    }

    override fun onPause() {
        super.onPause()
        MemoryOptimizer.onPause(webView)
        ConnectionKeeperService.onPulseListener = {
            webView?.evaluateJavascript(
                "(()=>{try{if(window.ServerSocket&&window.ServerSocket.connected){window.ServerSocket.emit('Ping');}}catch(e){}})();",
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
}
