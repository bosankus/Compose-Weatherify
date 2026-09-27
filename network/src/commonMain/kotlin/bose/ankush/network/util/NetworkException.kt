package bose.ankush.network.util

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.SerializationException

/**
 * Transport-level failure, classified into a stable [errorCode] the presentation layer can map to
 * copy a user should actually read (see `:feature:auth`'s `authErrorMessage` and `:feature:home`'s
 * `errorMessageFromException`).
 *
 * [message] stays technical on purpose — it is what gets attached to crash reports — so never show
 * it in the UI.
 */
class NetworkException(
    val errorCode: Int,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val SERVER_ERROR = 500
        const val SERVICE_UNAVAILABLE = 503

        const val NETWORK_UNAVAILABLE = 1000
        const val TIMEOUT = 1001
        const val UNKNOWN_HOST = 1002
        const val UNKNOWN_ERROR = 1999

        fun fromException(e: Exception): NetworkException =
            NetworkException(
                errorCode = classify(e),
                message = e.message ?: "Unknown error",
                cause = e,
            )

        /**
         * Classifies by exception type rather than by scanning the message for three digits: that
         * older heuristic picked up any number in the text (a timestamp, a byte count, the "500" in
         * a Ktor diagnostic) and just as often found nothing, so most failures ended up as
         * [UNKNOWN_ERROR].
         */
        private fun classify(e: Throwable): Int =
            when (e) {
                is NetworkException -> e.errorCode
                is ResponseException -> e.response.status.value
                is HttpRequestTimeoutException,
                is ConnectTimeoutException,
                is SocketTimeoutException,
                -> TIMEOUT

                // A body the client can't turn into the expected model — an HTML error page from a
                // broken deploy is the usual cause — is the server misbehaving, not the caller.
                is NoTransformationFoundException,
                is SerializationException,
                -> SERVER_ERROR

                else -> classifyByName(e)
            }

        /**
         * Connectivity failures come from kotlinx-io's `IOException` hierarchy, which this module
         * doesn't depend on directly; matching on the class name keeps it that way.
         */
        private fun classifyByName(e: Throwable): Int {
            val name = e::class.simpleName.orEmpty()
            return when {
                name == "UnresolvedAddressException" -> UNKNOWN_HOST
                name.endsWith("IOException") || name.endsWith("SocketException") -> NETWORK_UNAVAILABLE
                else -> e.cause?.takeIf { it !== e }?.let(::classify) ?: UNKNOWN_ERROR
            }
        }
    }
}
