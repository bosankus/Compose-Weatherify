package bose.ankush.weatherify.domain.model

/**
 * Domain model for air quality data
 */
data class AirQuality(
    var id: Long? = null,
    var aqi: Int? = 0,
    var co: Double? = 0.0,
    var no2: Double? = 0.0,
    var o3 : Double? = 0.0,
    var so2: Double? = 0.0,
    var pm10: Double? = 0.0,
    var pm25: Double? = 0.0,
)
