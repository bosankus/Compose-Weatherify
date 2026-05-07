package bose.ankush.weatherify.domain.model

/**
 * Domain model for weather forecast data
 */
data class WeatherForecast(
    val id: Long,
    val alerts: List<Alert?>? = listOf(),
    val current: Current? = null,
    val daily: List<Daily?>? = listOf(),
    val hourly: List<Hourly?>? = listOf(),
    val lastUpdated: Long = System.currentTimeMillis(),
) {
    data class Alert(
        val description: String?,
        val end: Long?,
        val event: String?,
        val sender_name: String?,
        val start: Long?,
    )

    data class Current(
        val clouds: Int?,
        val dt: Long?,
        val feels_like: Double?,
        val humidity: Int?,
        val pressure: Int?,
        val sunrise: Long?,
        val sunset: Long?,
        val temp: Double?,
        val uvi: Double?,
        val weather: List<WeatherCondition?>? = listOf(),
        val wind_gust: Double?,
        val wind_speed: Double?,
    )

    data class Daily(
        val clouds: Int?,
        val dew_point: Double?,
        val dt: Long?,
        val humidity: Int?,
        val pressure: Int?,
        val rain: Double?,
        val summary: String?,
        val sunrise: Long?,
        val sunset: Long?,
        val temp: Temp?,
        val uvi: Double?,
        val weather: List<WeatherCondition?>? = listOf(),
        val wind_gust: Double?,
        val wind_speed: Double?,
    ) {
        data class Temp(
            val day: Double?,
            val eve: Double?,
            val max: Double?,
            val min: Double?,
            val morn: Double?,
            val night: Double?,
        )
    }

    data class Hourly(
        val clouds: Int?,
        val dt: Long?,
        val feels_like: Double?,
        val humidity: Int?,
        val temp: Double?,
        val weather: List<WeatherCondition?>? = listOf(),
    )
}

/**
 * Domain model for weather condition
 */
data class WeatherCondition(
    val description: String = "",
    val icon: String = "",
    val id: Int,
    val main: String = "",
)
