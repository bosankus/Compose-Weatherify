package bose.ankush.commonui.web

/**
 * Classification of a web view load failure, used to show a message that matches what actually
 * went wrong instead of a single generic "Failed to load page".
 */
enum class WebViewErrorKind {
    NO_INTERNET,
    TIMEOUT,
    SERVER_ERROR,
    NOT_FOUND,
    ACCESS_DENIED,
    SECURE_CONNECTION,
    GENERIC,
}

/**
 * A user-facing description of a failed page load.
 *
 * @param kind What went wrong, used to pick the icon and copy.
 * @param toolbarTitle Short label shown in the top app bar while the error is visible. The page
 *   title is unusable in this state because servers commonly return their own error page title
 *   (e.g. "500 Server Error").
 * @param title Headline shown in the error overlay.
 * @param message Explanation shown under the headline.
 * @param detail Optional technical detail (status code, SSL reason) shown in small print.
 * @param isRetryable Whether retrying the same URL could plausibly succeed.
 */
data class WebViewError(
    val kind: WebViewErrorKind,
    val toolbarTitle: String,
    val title: String,
    val message: String,
    val detail: String? = null,
    val isRetryable: Boolean = true,
) {
    companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_GONE = 410
        const val HTTP_INTERNAL_SERVER_ERROR = 500
        const val HTTP_SERVICE_UNAVAILABLE = 503
        const val HTTP_GATEWAY_TIMEOUT = 504
        private const val HTTP_SERVER_ERROR_RANGE_START = 500
        private const val HTTP_SERVER_ERROR_RANGE_END = 599

        /** Maps an HTTP status returned for the main frame to a user-facing error. */
        fun fromHttpStatus(
            statusCode: Int,
            reasonPhrase: String? = null,
        ): WebViewError {
            val detail = buildDetail(statusCode, reasonPhrase)
            return when {
                statusCode == HTTP_REQUEST_TIMEOUT || statusCode == HTTP_GATEWAY_TIMEOUT ->
                    WebViewError(
                        kind = WebViewErrorKind.TIMEOUT,
                        toolbarTitle = "Timed out",
                        title = "This is taking too long",
                        message = "The site didn't respond in time. Please try again.",
                        detail = detail,
                    )

                statusCode == HTTP_SERVICE_UNAVAILABLE ->
                    WebViewError(
                        kind = WebViewErrorKind.SERVER_ERROR,
                        toolbarTitle = "Service unavailable",
                        title = "We're temporarily down",
                        message =
                            "This page is unavailable while we carry out maintenance. " +
                                "Please try again in a few minutes.",
                        detail = detail,
                    )

                statusCode in HTTP_SERVER_ERROR_RANGE_START..HTTP_SERVER_ERROR_RANGE_END ->
                    WebViewError(
                        kind = WebViewErrorKind.SERVER_ERROR,
                        toolbarTitle = "Server error",
                        title = "Something went wrong on our side",
                        message =
                            "Our server ran into a problem loading this page. " +
                                "It's not your device — please try again shortly.",
                        detail = detail,
                    )

                statusCode == HTTP_NOT_FOUND || statusCode == HTTP_GONE ->
                    WebViewError(
                        kind = WebViewErrorKind.NOT_FOUND,
                        toolbarTitle = "Page not found",
                        title = "We couldn't find this page",
                        message = "The page may have been moved or removed.",
                        detail = detail,
                        isRetryable = false,
                    )

                statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN ->
                    WebViewError(
                        kind = WebViewErrorKind.ACCESS_DENIED,
                        toolbarTitle = "Access denied",
                        title = "You don't have access to this page",
                        message = "This content is restricted or the link has expired.",
                        detail = detail,
                        isRetryable = false,
                    )

                else ->
                    WebViewError(
                        kind = WebViewErrorKind.GENERIC,
                        toolbarTitle = "Can't open page",
                        title = "This page couldn't be opened",
                        message = "Something went wrong loading this page. Please try again.",
                        detail = detail,
                    )
            }
        }

        fun noInternet(detail: String? = null) =
            WebViewError(
                kind = WebViewErrorKind.NO_INTERNET,
                toolbarTitle = "No connection",
                title = "You're offline",
                message = "Check your internet connection and try again.",
                detail = detail,
            )

        fun timeout(detail: String? = null) =
            WebViewError(
                kind = WebViewErrorKind.TIMEOUT,
                toolbarTitle = "Timed out",
                title = "This is taking too long",
                message = "The site didn't respond in time. Please try again.",
                detail = detail,
            )

        fun secureConnection(detail: String? = null) =
            WebViewError(
                kind = WebViewErrorKind.SECURE_CONNECTION,
                toolbarTitle = "Connection not secure",
                title = "We couldn't connect securely",
                message =
                    "The site's security certificate couldn't be verified, " +
                        "so the page was blocked to keep you safe.",
                detail = detail,
                isRetryable = false,
            )

        fun generic(detail: String? = null) =
            WebViewError(
                kind = WebViewErrorKind.GENERIC,
                toolbarTitle = "Can't open page",
                title = "This page couldn't be opened",
                message = "Something went wrong loading this page. Please try again.",
                detail = detail,
            )

        private fun buildDetail(
            statusCode: Int,
            reasonPhrase: String?,
        ): String =
            reasonPhrase
                ?.takeIf { it.isNotBlank() }
                ?.let { "Error $statusCode · $it" }
                ?: "Error $statusCode"
    }
}

/**
 * Picks the text for the web view's top app bar.
 *
 * A page's own title is unusable in two cases: when a load failed the title belongs to the
 * server's error page (e.g. "500 Server Error"), and when a page has no `<title>` at all the
 * platform substitutes the raw URL, which scrolls past as unreadable noise.
 *
 * @param error The current failure, if any.
 * @param pageTitle The title reported by the platform web view.
 * @param currentUrl The URL currently displayed, used to recognise a URL-as-title.
 * @param fallback Shown when there is no usable title.
 */
fun resolveWebViewTitle(
    error: WebViewError?,
    pageTitle: String,
    currentUrl: String,
    fallback: String,
): String {
    if (error != null) return error.toolbarTitle
    val title = pageTitle.trim()
    return if (title.isEmpty() || title.looksLikeUrl(currentUrl)) fallback else title
}

private fun String.looksLikeUrl(currentUrl: String): Boolean {
    if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) return true
    // WebView reports a truncated "host/path" form when a page has no <title>.
    val bareUrl =
        currentUrl
            .substringAfter("://")
            .removeSuffix("/")
    return bareUrl.isNotEmpty() && bareUrl.startsWith(removeSuffix("/"), ignoreCase = true)
}
