package bose.ankush.storage.model

/**
 * The one saved forecast and the air quality written with it in the same transaction.
 * [airQuality] is null only when no air-quality row exists.
 */
data class StoredWeather(
    val weather: WeatherData,
    val airQuality: AirQualityData?,
)
