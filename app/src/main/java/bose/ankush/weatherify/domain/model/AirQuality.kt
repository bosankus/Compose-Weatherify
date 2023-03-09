package bose.ankush.weatherify.domain.model

data class AirQuality(
    val aqi: Int? = 0,
    val co: Double? = 0.0,
    val no2: Double? = 0.0,
    val o3 : Double? = 0.0,
    val so2: Double? = 0.0,
    val pm10: Double? = 0.0,
    val pm25: Double? = 0.0
)