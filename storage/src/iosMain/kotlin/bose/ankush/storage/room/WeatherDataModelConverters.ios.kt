package bose.ankush.storage.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
actual class WeatherDataModelConverters actual constructor() {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = true
        }

    @TypeConverter
    actual fun toAlertJson(alerts: List<WeatherEntity.Alert?>?): String? = alerts?.let { json.encodeToString(it) }

    @TypeConverter
    actual fun fromAlertJson(alertString: String): List<WeatherEntity.Alert?> = decodeList(alertString) ?: emptyList()

    @TypeConverter
    actual fun toDailyWeatherJson(dailyWeatherReports: List<WeatherEntity.Daily?>?): String? =
        dailyWeatherReports?.let { json.encodeToString(it) }

    @TypeConverter
    actual fun fromDailyWeather(dailyWeatherString: String): List<WeatherEntity.Daily?> =
        decodeList(dailyWeatherString) ?: emptyList()

    @TypeConverter
    actual fun toHourlyWeatherJson(hourlyWeatherReports: List<WeatherEntity.Hourly?>?): String? =
        hourlyWeatherReports?.let { json.encodeToString(it) }

    @TypeConverter
    actual fun fromHourlyWeather(hourlyWeatherString: String): List<WeatherEntity.Hourly?> =
        decodeList(hourlyWeatherString) ?: emptyList()

    @TypeConverter
    actual fun toWeatherJson(weatherReports: List<Weather?>?): String? = weatherReports?.let { json.encodeToString(it) }

    @TypeConverter
    actual fun fromWeatherJson(weatherString: String): List<Weather?>? = decodeList(weatherString)

    private inline fun <reified T> decodeList(raw: String): List<T>? {
        if (raw.isBlank() || raw == "null") return null
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrNull()
    }
}
