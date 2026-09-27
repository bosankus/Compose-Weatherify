package bose.ankush.commonui.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun InAppWebView(
    url: String,
    modifier: Modifier,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val pageTitle = remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<WebViewError?>(null) }
    var isInitialLoad by remember { mutableStateOf(true) }
    val currentUrl = remember { mutableStateOf(url) }

    val webView =
        remember(context) {
            WebView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        }

    val openInBrowser = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, currentUrl.value.toUri()))
        } catch (_: ActivityNotFoundException) {
            // No browser available
        }
    }

    BackHandler(enabled = true) {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    WebViewTitle(
                        text =
                            resolveWebViewTitle(
                                error = loadError,
                                pageTitle = pageTitle.value,
                                currentUrl = currentUrl.value,
                                fallback = "Weatherify",
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webView.canGoBack()) webView.goBack() else onClose()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        loadError = null
                        isInitialLoad = true
                        webView.reload()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh page",
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, currentUrl.value)
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
                            contentDescription = "Share page",
                        )
                    }
                    IconButton(onClick = openInBrowser) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInBrowser,
                            contentDescription = "Open in browser",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
        ) {
            if (progress in 1..99 && loadError == null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier =
                        Modifier
                            .height(2.dp)
                            .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                AndroidView(
                    factory = { ctx ->
                        webView.apply {
                            configureWebView(
                                view = this,
                                onTitle = { pageTitle.value = it },
                                onProgress = { progress = it },
                                onExternalIntent = { intent ->
                                    try {
                                        ctx.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        // No handler available
                                    }
                                },
                                onError = { error ->
                                    loadError = error
                                    isInitialLoad = false
                                },
                                onNavigationStarted = {
                                    loadError = null
                                },
                                onPageFinished = {
                                    isInitialLoad = false
                                },
                            )
                        }
                    },
                    update = { view ->
                        if (view.url != url) {
                            currentUrl.value = url
                            view.loadUrl(url)
                        }
                    },
                )

                if (isInitialLoad && progress < 100 && loadError == null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                loadError?.let { error ->
                    WebViewErrorContent(
                        error = error,
                        onRetry = {
                            loadError = null
                            isInitialLoad = true
                            webView.reload()
                        },
                        onOpenInBrowser = openInBrowser.takeIf { !error.isRetryable },
                    )
                }
            }
        }
    }

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
    onError: (WebViewError) -> Unit = {},
    onNavigationStarted: () -> Unit = {},
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
        // SECURITY: Require user gesture for media playback to prevent unwanted autoplay
        mediaPlaybackRequiresUserGesture = true
    }

    // SECURITY: Only accept cookies from trusted legal content domains
    CookieManager.getInstance().setAcceptCookie(false)

    view.webViewClient =
        object : WebViewClient() {
            // Note: the error state is deliberately NOT reset in onPageStarted. WebView delivers
            // onReceivedHttpError with the response headers, i.e. *before* the error document
            // commits and onPageStarted fires, so resetting there would wipe the error before it
            // could ever render. A new navigation always passes through here instead.
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme ?: ""
                onNavigationStarted()
                return handleUrl(view, uri, scheme, onExternalIntent)
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?,
            ) {
                super.onPageFinished(view, url)
                onPageFinished()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame != true) return
                val detail = error?.description?.toString()?.takeIf { it.isNotBlank() }
                onError(
                    when (error?.errorCode) {
                        ERROR_HOST_LOOKUP,
                        ERROR_CONNECT,
                        ERROR_IO,
                        ERROR_PROXY_AUTHENTICATION,
                        -> WebViewError.noInternet(detail)

                        ERROR_TIMEOUT -> WebViewError.timeout(detail)

                        ERROR_FAILED_SSL_HANDSHAKE -> WebViewError.secureConnection(detail)

                        ERROR_FILE_NOT_FOUND -> WebViewError.fromHttpStatus(404)

                        ERROR_AUTHENTICATION,
                        ERROR_UNSUPPORTED_AUTH_SCHEME,
                        -> WebViewError.fromHttpStatus(401)

                        else -> WebViewError.generic(detail)
                    },
                )
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame != true) return
                onError(
                    WebViewError.fromHttpStatus(
                        statusCode = errorResponse?.statusCode ?: 0,
                        reasonPhrase = errorResponse?.reasonPhrase,
                    ),
                )
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                super.onReceivedSslError(view, handler, error)
                handler?.cancel()
                val detail =
                    when (error?.primaryError) {
                        SslError.SSL_EXPIRED -> "Certificate expired"
                        SslError.SSL_IDMISMATCH -> "Certificate hostname mismatch"
                        SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                        SslError.SSL_UNTRUSTED -> "Certificate not trusted"
                        else -> null
                    }
                onError(WebViewError.secureConnection(detail))
            }
        }

    view.webChromeClient =
        object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView?,
                newProgress: Int,
            ) {
                super.onProgressChanged(view, newProgress)
                onProgress(newProgress)
            }

            override fun onReceivedTitle(
                view: WebView?,
                title: String?,
            ) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank()) onTitle(title)
            }
        }
}

private fun handleUrl(
    webView: WebView?,
    uri: Uri,
    scheme: String,
    onExternalIntent: (Intent) -> Unit,
): Boolean {
    when (scheme.lowercase()) {
        "http", "https" -> {
            if (isWhitelistedUrl(uri)) {
                webView?.loadUrl(uri.toString())
            } else {
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
    return true
}

private fun isWhitelistedUrl(uri: Uri): Boolean {
    val host = uri.host?.lowercase() ?: return false
    val whitelistedDomains =
        setOf(
            "data.androidplay.in",
        )
    return whitelistedDomains.any { trustedDomain ->
        host == trustedDomain || host.endsWith(".$trustedDomain")
    }
}
