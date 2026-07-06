package bose.ankush.home.presentation.home.util

private const val AQI_SINGLE_DIGIT_MAX = 9

internal object AirQualityIndexAnalyser {
    internal fun getAQIAnalysedText(aqi: Int): Pair<String, Int> =
        when (aqi) {
            1 -> Pair("Air quality is Good", aqi)
            2 -> Pair("Air quality is fair", aqi)
            3 -> Pair("Air quality is moderate", aqi)
            4 -> Pair("Air quality is poor", aqi)
            5 -> Pair("Air quality is Very Unhealthy", aqi)
            else -> Pair("Air quality is Hazardous", aqi)
        }

    internal fun Int.getFormattedAQI(): String =
        if (this in 0..AQI_SINGLE_DIGIT_MAX) {
            "0$this"
        } else {
            "$this"
        }
}
