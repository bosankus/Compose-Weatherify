package bose.ankush.weatherify.common

import android.content.Context
import androidx.annotation.StringRes

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
