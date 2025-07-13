package bose.ankush.network.model

import kotlinx.serialization.Serializable

/**
 * Domain model for weather forecast data
 */
@Serializable
data class WeatherForecast(
    val id: Long = 0, // Default value to handle missing id in API response
    val alerts: List<Alert?>? = listOf(),
    val current: Current? = null,
    val daily: List<Daily?>? = listOf(),
    val hourly: List<Hourly?>? = listOf(),
    val lastUpdated: Long = 0,
) {
    @Serializable
    data class Alert(
        val description: String? = null,
        val end: Int? = null,
        val event: String? = null,
        val sender_name: String? = null,
        val start: Int? = null,
    )

    @Serializable
    data class Current(
        val clouds: Int? = null,
        val dt: Long? = null,
        val feels_like: Double? = null,
        val humidity: Int? = null,
        val pressure: Int? = null,
        val sunrise: Int? = null,
        val sunset: Int? = null,
        val temp: Double? = null,
        val uvi: Double? = null,
        val weather: List<WeatherCondition?>? = listOf(),
        val wind_gust: Double? = null,
        val wind_speed: Double? = null
    )

    @Serializable
    data class Daily(
        val clouds: Int? = null,
        val dew_point: Double? = null,
        val dt: Long? = null,
        val humidity: Int? = null,
        val pressure: Int? = null,
        val rain: Double? = null,
        val summary: String? = null,
        val sunrise: Int? = null,
        val sunset: Int? = null,
        val temp: Temp? = null,
        val uvi: Double? = null,
        val weather: List<WeatherCondition?>? = listOf(),
        val wind_gust: Double? = null,
        val wind_speed: Double? = null
    ) {
        @Serializable
        data class Temp(
            val day: Double? = null,
            val eve: Double? = null,
            val max: Double? = null,
            val min: Double? = null,
            val morn: Double? = null,
            val night: Double? = null
        )
    }

    @Serializable
    data class Hourly(
        val clouds: Int? = null,
        val dt: Long? = null,
        val feels_like: Double? = null,
        val humidity: Int? = null,
        val temp: Double? = null,
        val weather: List<WeatherCondition?>? = listOf(),
    )
}
