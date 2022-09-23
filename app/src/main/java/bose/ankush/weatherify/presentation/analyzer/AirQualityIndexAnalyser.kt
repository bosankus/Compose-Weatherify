package bose.ankush.weatherify.presentation.analyzer

object AirQualityIndexAnalyser {

    /**
     * Used to analyse the air quality index number,
     * and generate a string accordingly for UI to show
     */
    fun getAQIAnalysedText(aqi: Int): String {
        return when(aqi) {
            in 0..50-> "Air quality is Good at this moment"
            in 51..100 -> "Air quality is Moderate at this moment"
            in 101..150 -> "Air quality is Unhealthy for sensitive group at this moment"
            in 151..200 -> "Air quality is Unhealthy at this moment"
            in 201..300 -> "Air quality is Very Unhealthy at this moment"
            else -> "Air quality is Hazardous at this moment"
        }
    }


}