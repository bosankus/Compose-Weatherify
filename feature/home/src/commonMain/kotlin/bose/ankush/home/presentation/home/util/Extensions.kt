package bose.ankush.home.presentation.home.util

import kotlin.math.roundToInt

private const val KELVIN_OFFSET = 273.15
private const val WEATHER_IMG_URL = "https://openweathermap.org/img/wn/"

internal fun Double.toCelsius() = (this - KELVIN_OFFSET).roundToInt().toString()

internal fun String.getIconUrl(size: String = "@2x.png") = "$WEATHER_IMG_URL$this$size"

internal fun String.formatTextCapitalization() = replaceFirstChar { it.uppercaseChar() }

internal fun String.wrapText(): String {
    val words = this.split(" ")
    return if (words.size == 2) {
        "${words[0]}\n${words[1]}"
    } else {
        this
    }
}
