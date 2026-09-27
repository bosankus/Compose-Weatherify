package bose.ankush.commonui.web

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Full-screen overlay describing why a page failed to load.
 *
 * Shared by the Android and iOS [InAppWebView] actuals so both platforms show identical copy for
 * the same [WebViewError].
 *
 * @param error The classified failure to describe.
 * @param onRetry Reloads the current URL. Only surfaced when [WebViewError.isRetryable].
 * @param onOpenInBrowser Optional escape hatch shown for failures retrying won't fix.
 */
@Composable
fun WebViewErrorContent(
    error: WebViewError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenInBrowser: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                // Opaque: the server's own error page must not show through.
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = error.kind.icon(),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = error.kind.tint(),
        )
        Text(
            text = error.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        error.detail?.takeIf { it.isNotBlank() }?.let { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (error.isRetryable) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text("Try again")
            }
        }
        if (onOpenInBrowser != null) {
            TextButton(
                onClick = onOpenInBrowser,
                modifier = Modifier.padding(top = if (error.isRetryable) 4.dp else 24.dp),
            ) {
                Text("Open in browser")
            }
        }
    }
}

/**
 * Top app bar title for the web view. Long titles scroll horizontally instead of being cut off
 * mid-word by an ellipsis.
 */
@Composable
fun WebViewTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
    )
}

@Composable
private fun WebViewErrorKind.icon() =
    when (this) {
        WebViewErrorKind.NO_INTERNET -> Icons.Outlined.CloudOff
        WebViewErrorKind.TIMEOUT -> Icons.Outlined.Schedule
        WebViewErrorKind.SERVER_ERROR -> Icons.Outlined.Dns
        WebViewErrorKind.NOT_FOUND -> Icons.Outlined.SearchOff
        WebViewErrorKind.ACCESS_DENIED -> Icons.Outlined.Lock
        WebViewErrorKind.SECURE_CONNECTION -> Icons.Outlined.GppMaybe
        WebViewErrorKind.GENERIC -> Icons.Outlined.ErrorOutline
    }

@Composable
private fun WebViewErrorKind.tint() =
    when (this) {
        // A server outage or a dropped connection isn't the user's fault and isn't destructive,
        // so it reads as informational rather than as a red error.
        WebViewErrorKind.NO_INTERNET,
        WebViewErrorKind.TIMEOUT,
        WebViewErrorKind.SERVER_ERROR,
        WebViewErrorKind.NOT_FOUND,
        -> MaterialTheme.colorScheme.onSurfaceVariant

        WebViewErrorKind.ACCESS_DENIED,
        WebViewErrorKind.SECURE_CONNECTION,
        WebViewErrorKind.GENERIC,
        -> MaterialTheme.colorScheme.error
    }
