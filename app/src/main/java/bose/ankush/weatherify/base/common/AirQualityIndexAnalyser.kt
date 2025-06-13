package bose.ankush.weatherify.base.common

object AirQualityIndexAnalyser {

    /**
     * Used to analyse the air quality index number,
     * and generate a string accordingly for UI to show
     */
    internal fun getAQIAnalysedText(aqi: Int): Pair<String, Int> {
        return when (aqi) {
            1 -> Pair("Air quality is Good", aqi)
            2 -> Pair("Air quality is fair", aqi)
            3 -> Pair("Air quality is moderate", aqi)
            4 -> Pair("Air quality is poor", aqi)
            5 -> Pair("Air quality is Very Unhealthy", aqi)
            else -> Pair("Air quality is Hazardous", aqi)
        }
    }

    /**
     * This method is actually for making look pretty by adding
     * adding `0` to single digit number
     */
    internal fun Int.getFormattedAQI(): String {
        return if (this in 0..9) "0$this"
        else "$this"
    }
}