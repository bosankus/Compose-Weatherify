package bose.ankush.weatherify.domain.model

data class AirQuality(
    val latLang: String?,
    val aqi: Int?,
    val co: Int?,
    val no2: Double?,
    val o3 : Double?,
    val so2: Double?,
    val pm10: Double?,
    val pm25: Double?
)
