@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.commonui.web

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
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
    var loadError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
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

    delegate.initialUrl = url
    delegate.onLoadStart = {
        isLoading = true
        loadError = false
    }
    delegate.onLoadFinish = { wv ->
        isLoading = false
        canGoBack = wv.canGoBack
        pageTitle = wv.title ?: ""
        currentUrl = wv.URL?.absoluteString ?: url
    }
    delegate.onLoadError = { msg ->
        isLoading = false
        loadError = true
        errorMessage = msg
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
                    Text(
                        text = pageTitle.ifBlank { "Weatherify" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    IconButton(onClick = { webView.reload() }) {
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
                    IconButton(onClick = {
                        NSURL.URLWithString(currentUrl)?.let { nsUrl ->
                            @Suppress("DEPRECATION")
                            UIApplication.sharedApplication.openURL(nsUrl)
                        }
                    }) {
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
            if (isLoading) {
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

                if (isLoading) {
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

                if (loadError) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                modifier =
                                    Modifier
                                        .size(64.dp)
                                        .padding(bottom = 16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Failed to load page",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            if (errorMessage.isNotBlank()) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 24.dp),
                                )
                            }
                            Button(
                                onClick = {
                                    loadError = false
                                    errorMessage = ""
                                    isLoading = true
                                    webView.reload()
                                },
                            ) {
                                Text("Retry")
                            }
                        }
                    }
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
    var onLoadError: (String) -> Unit = {}
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
        onLoadError(withError.localizedDescription)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError,
    ) {
        onLoadError(withError.localizedDescription)
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
