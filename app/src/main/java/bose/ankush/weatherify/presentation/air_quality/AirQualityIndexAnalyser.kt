package bose.ankush.weatherify.presentation.air_quality

object AirQualityIndexAnalyser {

    /**
     * Used to analyse the air quality index number,
     * and generate a string accordingly for UI to show
     */
    fun getAQIAnalysedText(aqi: Int): Pair<String, Int> {
        return when(aqi) {
            in 0..50-> Pair("Air quality is Good at this moment", aqi)
            in 51..100 -> Pair("Air quality is Moderate at this moment", aqi)
            in 101..150 -> Pair("Air quality is Unhealthy for sensitive group at this moment", aqi)
            in 151..200 -> Pair("Air quality is Unhealthy at this moment", aqi)
            in 201..300 -> Pair("Air quality is Very Unhealthy at this moment", aqi)
            else -> Pair("Air quality is Hazardous at this moment", aqi)
        }
    }


}