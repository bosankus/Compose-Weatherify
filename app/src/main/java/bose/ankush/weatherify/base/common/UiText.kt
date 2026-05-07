package bose.ankush.weatherify.base.common

import android.content.Context
import androidx.annotation.StringRes
import bose.ankush.network.common.NetworkException
import bose.ankush.weatherify.R

sealed class UiText {
    data class DynamicText(
        val value: String,
    ) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: String,
    ) : UiText()

    fun asString(context: Context): String =
        when (this) {
            is DynamicText -> value
            is StringResource -> context.getString(resId, *args)
        }
}

/**
 * Maps error codes to user-friendly messages
 * @param errorCode The HTTP or custom error code
 * @return A user-friendly error message as a StringResource
 */
fun errorResponse(errorCode: Int): UiText.StringResource =
    when (errorCode) {
        // HTTP error codes
        NetworkException.BAD_REQUEST -> UiText.StringResource(resId = R.string.city_error_txt)
        NetworkException.UNAUTHORIZED -> UiText.StringResource(resId = R.string.unauthorised_access_txt)
        NetworkException.FORBIDDEN -> UiText.StringResource(resId = R.string.unauthorised_access_txt)
        NetworkException.NOT_FOUND -> UiText.StringResource(resId = R.string.city_error_txt)
        NetworkException.SERVER_ERROR -> UiText.StringResource(resId = R.string.server_error_txt)
        NetworkException.SERVICE_UNAVAILABLE -> UiText.StringResource(resId = R.string.server_error_txt)

        // Network-specific error codes
        NetworkException.NETWORK_UNAVAILABLE -> UiText.StringResource(resId = R.string.network_unavailable_txt)
        NetworkException.TIMEOUT -> UiText.StringResource(resId = R.string.network_timeout_txt)
        NetworkException.UNKNOWN_HOST -> UiText.StringResource(resId = R.string.network_unavailable_txt)

        // Default case
        else -> UiText.StringResource(resId = R.string.general_error_txt)
    }

/**
 * Maps an exception to a user-friendly message
 * @param exception The exception to map
 * @return A user-friendly error message
 */
fun errorResponseFromException(exception: Exception): UiText =
    when (exception) {
        is NetworkException -> errorResponse(exception.errorCode)
        else -> UiText.StringResource(resId = R.string.general_error_txt)
    }
