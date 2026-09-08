package com.hemang.strengthfitness

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return loadLocalAsset(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return openExternalIfNeeded(Uri.parse(url))
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return openExternalIfNeeded(request.url)
            }
        }

        // The HTML app is bundled inside the APK and served from a local HTTPS-like origin.
        // This avoids relying on AndroidX WebViewAssetLoader and keeps the wrapper lightweight.
        webView.loadUrl("https://strengthfitness.local/assets/index.html")
    }

    private fun openExternalIfNeeded(uri: Uri): Boolean {
        val host = uri.host ?: ""
        val isAppPage = uri.scheme == "https" && host == "strengthfitness.local"
        if (isAppPage) return false

        if (uri.scheme == "http" || uri.scheme == "https") {
            return try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    private fun loadLocalAsset(uri: Uri): WebResourceResponse? {
        if (uri.scheme != "https" || uri.host != "strengthfitness.local") return null

        val path = uri.path ?: "/assets/index.html"
        val assetPath = path.removePrefix("/assets/")
        if (assetPath.isBlank() || assetPath.contains("..")) return null

        val mime = when {
            assetPath.endsWith(".html", true) -> "text/html"
            assetPath.endsWith(".js", true) -> "application/javascript"
            assetPath.endsWith(".css", true) -> "text/css"
            assetPath.endsWith(".json", true) -> "application/json"
            assetPath.endsWith(".png", true) -> "image/png"
            assetPath.endsWith(".jpg", true) || assetPath.endsWith(".jpeg", true) -> "image/jpeg"
            assetPath.endsWith(".svg", true) -> "image/svg+xml"
            assetPath.endsWith(".webp", true) -> "image/webp"
            else -> "application/octet-stream"
        }

        return try {
            val input = assets.open(assetPath)
            WebResourceResponse(mime, "UTF-8", input)
        } catch (_: IOException) {
            null
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
