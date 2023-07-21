package bose.ankush.weatherify.base.common

import android.content.Context
import androidx.annotation.StringRes
import bose.ankush.weatherify.R

sealed class UiText {
    data class DynamicText(val value: String) : UiText()
    class StringResource(@StringRes val resId: Int, vararg val args: String) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicText -> value
            is StringResource -> context.getString(resId, *args)
        }
    }
}

fun errorResponse(errorCode: Int): UiText.StringResource {
    return when (errorCode) {
        401 -> UiText.StringResource(resId = R.string.unauthorised_access_txt)
        400, 404 -> UiText.StringResource(resId = R.string.city_error_txt)
        500 -> UiText.StringResource(resId = R.string.server_error_txt)
        else -> UiText.StringResource(resId = R.string.general_error_txt)
    }
}
