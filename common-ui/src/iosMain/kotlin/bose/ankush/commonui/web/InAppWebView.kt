@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.commonui.web

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun InAppWebView(
    url: String,
    modifier: Modifier,
    onClose: () -> Unit,
) {
    var pageTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<WebViewError?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(url) }

    // Hold a strong reference to the delegate — WKWebView.navigationDelegate is weak in ObjC
    val delegate = remember { InAppWebViewDelegate() }

    val webView =
        remember {
            val config =
                WKWebViewConfiguration().apply {
                    // Non-persistent storage: equivalent to CookieManager.setAcceptCookie(false) on Android
                    websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
                }
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                navigationDelegate = delegate
            }
        }

    val openInBrowser = {
        NSURL.URLWithString(currentUrl)?.let { nsUrl ->
            @Suppress("DEPRECATION")
            UIApplication.sharedApplication.openURL(nsUrl)
        }
        Unit
    }

    delegate.initialUrl = url
    delegate.onLoadStart = {
        isLoading = true
        loadError = null
    }
    delegate.onLoadFinish = { wv ->
        isLoading = false
        canGoBack = wv.canGoBack
        pageTitle = wv.title ?: ""
        currentUrl = wv.URL?.absoluteString ?: url
    }
    delegate.onLoadError = { error ->
        isLoading = false
        loadError = error
    }
    delegate.onNavigationBlocked = {
        onClose()
    }

    LaunchedEffect(url) {
        NSURL.URLWithString(url)?.let { nsUrl ->
            webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    WebViewTitle(
                        text =
                            resolveWebViewTitle(
                                error = loadError,
                                pageTitle = pageTitle,
                                currentUrl = currentUrl,
                                fallback = "Weatherify",
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canGoBack) webView.goBack() else onClose()
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
                        isLoading = true
                        webView.reload()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh page",
                        )
                    }
                    IconButton(onClick = {
                        val activityVC =
                            UIActivityViewController(
                                activityItems = listOf(currentUrl),
                                applicationActivities = null,
                            )
                        @Suppress("DEPRECATION")
                        UIApplication.sharedApplication.keyWindow
                            ?.rootViewController
                            ?.presentViewController(activityVC, animated = true, completion = null)
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
            if (isLoading && loadError == null) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .height(2.dp)
                            .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                UIKitView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                    update = {},
                )

                if (isLoading && loadError == null) {
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
                            isLoading = true
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
            webView.stopLoading()
            webView.navigationDelegate = null
        }
    }
}

/**
 * WKNavigationDelegate implementation that forwards load events to Compose state setters.
 * Kept as a named class so that a strong Kotlin reference can be held (WKWebView.navigationDelegate
 * is a weak ObjC reference and would otherwise be immediately deallocated).
 *
 * URL whitelist enforcement: decidePolicyForNavigationAction validates navigation URLs
 * against the same trusted domain list used on Android (e.g., data.androidplay.in).
 */
private class InAppWebViewDelegate :
    NSObject(),
    WKNavigationDelegateProtocol {
    var onLoadStart: () -> Unit = {}
    var onLoadFinish: (WKWebView) -> Unit = {}
    var onLoadError: (WebViewError) -> Unit = {}
    var onNavigationBlocked: () -> Unit = {}
    var initialUrl: String = ""

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didStartProvisionalNavigation: WKNavigation?,
    ) {
        onLoadStart()
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?,
    ) {
        onLoadFinish(webView)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError,
    ) {
        reportError(withError)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError,
    ) {
        reportError(withError)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val requestUrl = decidePolicyForNavigationAction.request.URL?.absoluteString ?: ""

        if (requestUrl == initialUrl) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }

        if (isWhitelistedUrl(requestUrl)) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        } else {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            onNavigationBlocked()
        }
    }

    /**
     * WKWebView renders HTTP error pages instead of failing the navigation, so the status code for
     * the main frame is only visible here. Anything outside 2xx/3xx is surfaced as an error.
     */
    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationResponse: WKNavigationResponse,
        decisionHandler: (WKNavigationResponsePolicy) -> Unit,
    ) {
        decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)

        if (!decidePolicyForNavigationResponse.forMainFrame) return
        val httpResponse = decidePolicyForNavigationResponse.response as? NSHTTPURLResponse ?: return
        val statusCode = httpResponse.statusCode.toInt()
        if (statusCode >= 400) {
            onLoadError(
                WebViewError.fromHttpStatus(
                    statusCode = statusCode,
                    reasonPhrase = NSHTTPURLResponse.localizedStringForStatusCode(httpResponse.statusCode),
                ),
            )
        }
    }

    private fun reportError(error: NSError) {
        val detail = error.localizedDescription.takeIf { it.isNotBlank() }
        onLoadError(
            when (error.code) {
                // Navigation we cancelled ourselves (e.g. a blocked URL) — not a failure to show.
                NSURL_ERROR_CANCELLED -> return

                NSURL_ERROR_NOT_CONNECTED_TO_INTERNET,
                NSURL_ERROR_NETWORK_CONNECTION_LOST,
                NSURL_ERROR_CANNOT_FIND_HOST,
                NSURL_ERROR_CANNOT_CONNECT_TO_HOST,
                NSURL_ERROR_DNS_LOOKUP_FAILED,
                -> WebViewError.noInternet(detail)

                NSURL_ERROR_TIMED_OUT -> WebViewError.timeout(detail)

                NSURL_ERROR_SECURE_CONNECTION_FAILED,
                NSURL_ERROR_SERVER_CERTIFICATE_HAS_BAD_DATE,
                NSURL_ERROR_SERVER_CERTIFICATE_UNTRUSTED,
                NSURL_ERROR_SERVER_CERTIFICATE_HAS_UNKNOWN_ROOT,
                NSURL_ERROR_SERVER_CERTIFICATE_NOT_YET_VALID,
                NSURL_ERROR_CLIENT_CERTIFICATE_REJECTED,
                -> WebViewError.secureConnection(detail)

                NSURL_ERROR_BAD_SERVER_RESPONSE ->
                    WebViewError.fromHttpStatus(WebViewError.HTTP_INTERNAL_SERVER_ERROR)

                NSURL_ERROR_FILE_DOES_NOT_EXIST ->
                    WebViewError.fromHttpStatus(WebViewError.HTTP_NOT_FOUND)

                else -> WebViewError.generic(detail)
            },
        )
    }

    private fun isWhitelistedUrl(urlString: String): Boolean {
        val url = NSURL.URLWithString(urlString) ?: return false
        val host = url.host?.lowercase() ?: return false
        val whitelistedDomains =
            setOf(
                "data.androidplay.in",
            )
        return whitelistedDomains.any { trustedDomain ->
            host == trustedDomain || host.endsWith(".$trustedDomain")
        }
    }
}

// NSURLErrorDomain codes (CFNetworkErrors). Declared at file level because a Kotlin companion
// object is not allowed inside a subclass of an Objective-C type.
private const val NSURL_ERROR_CANCELLED = -999L
private const val NSURL_ERROR_BAD_SERVER_RESPONSE = -1011L
private const val NSURL_ERROR_TIMED_OUT = -1001L
private const val NSURL_ERROR_CANNOT_FIND_HOST = -1003L
private const val NSURL_ERROR_CANNOT_CONNECT_TO_HOST = -1004L
private const val NSURL_ERROR_NETWORK_CONNECTION_LOST = -1005L
private const val NSURL_ERROR_DNS_LOOKUP_FAILED = -1006L
private const val NSURL_ERROR_NOT_CONNECTED_TO_INTERNET = -1009L
private const val NSURL_ERROR_FILE_DOES_NOT_EXIST = -1100L
private const val NSURL_ERROR_SECURE_CONNECTION_FAILED = -1200L
private const val NSURL_ERROR_SERVER_CERTIFICATE_HAS_BAD_DATE = -1201L
private const val NSURL_ERROR_SERVER_CERTIFICATE_UNTRUSTED = -1202L
private const val NSURL_ERROR_SERVER_CERTIFICATE_HAS_UNKNOWN_ROOT = -1203L
private const val NSURL_ERROR_SERVER_CERTIFICATE_NOT_YET_VALID = -1204L
private const val NSURL_ERROR_CLIENT_CERTIFICATE_REJECTED = -1205L
