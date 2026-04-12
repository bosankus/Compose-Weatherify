package bose.ankush.commonui.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

/**
 * Android actual: security-hardened WebView wrapped in AndroidView.
 *
 * Note: State assignments (pageTitle, progress, currentUrl) are read through Compose's
 * recomposition system, so IDE warnings about unused assignments are false positives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun InAppWebView(
    url: String,
    modifier: Modifier,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    var pageTitle by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isInitialLoad by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(url) }

    // Keep a single WebView instance across recompositions
    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    BackHandler(enabled = true) {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle.ifBlank { "Weatherify" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webView.canGoBack()) webView.goBack() else onClose()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView.reload() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh page"
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, currentUrl)
                            type = "text/plain"
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share URL")
                        try {
                            context.startActivity(chooser)
                        } catch (_: ActivityNotFoundException) {
                            // No app available to handle share
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share page"
                        )
                    }
                    IconButton(onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, currentUrl.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // No browser available
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInBrowser,
                            contentDescription = "Open in browser"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AndroidView(
                    factory = { ctx ->
                        webView.apply {
                            // Note: State assignments in these callbacks (pageTitle, progress) trigger
                            // Compose recomposition. IDE warnings about unused assignments are false positives.
                            configureWebView(
                                view = this,
                                onTitle = { pageTitle = it },
                                onProgress = { progress = it },
                                onExternalIntent = { intent ->
                                    try {
                                        ctx.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        // No handler available
                                    }
                                },
                                onError = { message ->
                                    loadError = true
                                    errorMessage = message
                                },
                                onPageFinished = {
                                    isInitialLoad = false
                                }
                            )
                        }
                    },
                    update = { view ->
                        if (view.url != url) {
                            //noinspection ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
                            currentUrl = url
                            view.loadUrl(url)
                        }
                    }
                )

                // Loading overlay during initial page load
                if (isInitialLoad && progress < 100) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Error overlay when page fails to load
                if (loadError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(bottom = 16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Failed to load page",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (errorMessage.isNotBlank()) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    loadError = false
                                    errorMessage = ""
                                    isInitialLoad = true
                                    webView.reload()
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }

    // Clean up WebView resources when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            } catch (_: Exception) {
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(
    view: WebView,
    onTitle: (String) -> Unit,
    onProgress: (Int) -> Unit,
    onExternalIntent: (Intent) -> Unit,
    onError: (String) -> Unit = {},
    onPageFinished: () -> Unit = {},
) {
    with(view.settings) {
        // SECURITY: Disable JavaScript to prevent XSS attacks in legal documents
        javaScriptEnabled = false
        // SECURITY: Disable DOM storage to prevent credential/token theft
        domStorageEnabled = false
        @Suppress("DEPRECATION")
        databaseEnabled = false
        // SECURITY: Never allow mixed content (HTTP on HTTPS) to prevent MITM attacks
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        cacheMode = WebSettings.LOAD_DEFAULT
        builtInZoomControls = true
        displayZoomControls = false
        useWideViewPort = true
        loadWithOverviewMode = true
        // SECURITY: Disable multiple windows to prevent popup injection attacks
        setSupportMultipleWindows(false)
        mediaPlaybackRequiresUserGesture = false
    }

    // SECURITY: Only accept cookies from trusted legal content domains
    CookieManager.getInstance().setAcceptCookie(false)

    view.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme ?: ""
            return handleUrl(view, uri, scheme, onExternalIntent)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            val errorDesc = error?.description?.toString() ?: "Unknown error"
            onError("Failed to load: $errorDesc")
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: android.webkit.WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            val statusCode = errorResponse?.statusCode ?: 0
            val reason = errorResponse?.reasonPhrase ?: "Unknown error"
            onError("HTTP Error $statusCode: $reason")
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: android.net.http.SslError?
        ) {
            super.onReceivedSslError(view, handler, error)
            handler?.cancel()
            val errorMsg = when (error?.primaryError) {
                android.net.http.SslError.SSL_EXPIRED -> "SSL certificate expired"
                android.net.http.SslError.SSL_IDMISMATCH -> "SSL certificate hostname mismatch"
                android.net.http.SslError.SSL_NOTYETVALID -> "SSL certificate not yet valid"
                android.net.http.SslError.SSL_UNTRUSTED -> "SSL certificate not trusted"
                else -> "SSL certificate error"
            }
            onError(errorMsg)
        }
    }

    view.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (!title.isNullOrBlank()) onTitle(title)
        }

        // Handle target=_blank and window.open to keep in same WebView
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            val context = view?.context ?: return false
            val tempWebView = WebView(context)
            tempWebView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(v, url, favicon)
                    if (!url.isNullOrBlank()) {
                        view.loadUrl(url)
                        tempWebView.destroy()
                    }
                }
            }
            transport.webView = tempWebView
            resultMsg.sendToTarget()
            return true
        }
    }
}

private fun handleUrl(
    webView: WebView?,
    uri: Uri,
    scheme: String,
    onExternalIntent: (Intent) -> Unit
): Boolean {
    when (scheme.lowercase()) {
        "http", "https" -> {
            if (isWhitelistedUrl(uri)) {
                webView?.loadUrl(uri.toString())
            } else {
                // Reject URLs from untrusted domains
                Log.w("InAppWebView", "Blocked untrusted URL: $uri")
            }
        }
        "tel", "mailto", "geo", "sms", "intent" -> {
            onExternalIntent(Intent(Intent.ACTION_VIEW, uri))
        }
        else -> {
            onExternalIntent(Intent(Intent.ACTION_VIEW, uri))
        }
    }
    // Always return true to indicate we handled the URL loading
    return true
}

/**
 * Validate URL against whitelist of trusted domains.
 * Only allows loading content from whitelisted legal document hosts.
 */
private fun isWhitelistedUrl(uri: Uri): Boolean {
    val host = uri.host?.lowercase() ?: return false
    val whitelistedDomains = setOf(
        "data.androidplay.in",     // Terms, Privacy Policy
    )
    return whitelistedDomains.any { trustedDomain ->
        host == trustedDomain || host.endsWith(".$trustedDomain")
    }
}
