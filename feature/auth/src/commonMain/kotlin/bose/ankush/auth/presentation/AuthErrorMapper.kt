package bose.ankush.auth.presentation

import bose.ankush.auth.generated.resources.Res
import bose.ankush.auth.generated.resources.auth_error_credentials
import bose.ankush.auth.generated.resources.auth_error_generic
import bose.ankush.auth.generated.resources.auth_error_network
import bose.ankush.auth.generated.resources.auth_error_server
import bose.ankush.auth.generated.resources.auth_error_timeout
import bose.ankush.network.util.NetworkException
import org.jetbrains.compose.resources.getString

/**
 * Turns a thrown failure into copy a signed-out user can act on.
 *
 * Exception messages are never surfaced as-is: Ktor's own text (e.g. the multi-line
 * `NoTransformationFoundException` dump a 500 HTML error page produces) is meaningless to a user
 * and leaks endpoints and payload types into the UI. The technical detail is reported to
 * Crashlytics by the caller instead.
 */
internal suspend fun authErrorMessage(throwable: Throwable): String =
    when (val code = (throwable as? NetworkException)?.errorCode) {
        null -> getString(Res.string.auth_error_generic)
        NetworkException.UNAUTHORIZED, NetworkException.FORBIDDEN ->
            getString(Res.string.auth_error_credentials)

        NetworkException.NETWORK_UNAVAILABLE, NetworkException.UNKNOWN_HOST ->
            getString(Res.string.auth_error_network)

        NetworkException.TIMEOUT -> getString(Res.string.auth_error_timeout)
        else ->
            if (code >= NetworkException.SERVER_ERROR) {
                getString(Res.string.auth_error_server)
            } else {
                getString(Res.string.auth_error_generic)
            }
    }
