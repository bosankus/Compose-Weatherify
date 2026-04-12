package bose.ankush.commonui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cross-platform in-app web view composable (CMP/KMP).
 *
 * Android: Uses Android android.webkit.WebView wrapped in androidx.compose.ui.viewinterop.AndroidView.
 *   - JavaScript disabled, cookies disabled, mixed-content blocked (security hardened).
 *   - URL whitelist enforced via android.webkit.WebViewClient.
 *
 * iOS: Uses platform.WebKit.WKWebView wrapped in androidx.compose.ui.viewinterop.UIKitView.
 *   - Non-persistent website data store (no cookie/cache persistence).
 *   - Navigation delegate tracks load state and errors.
 *
 * @param url The URL to load on first display.
 * @param modifier Optional modifier for the root layout.
 * @param onClose Called when the user navigates back past the first page or taps the back icon.
 */
@Composable
expect fun InAppWebView(
    url: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
)
