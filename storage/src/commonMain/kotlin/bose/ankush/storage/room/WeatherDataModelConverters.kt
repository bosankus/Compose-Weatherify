package bose.ankush.storage.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter

/**
 * Platform TypeConverters for weather nested JSON columns.
 *
 * Android actual keeps the historical Gson wire format so existing Room rows remain readable.
 * iOS actual uses kotlinx.serialization (fresh DB; no legacy rows).
 */
@ProvidedTypeConverter
expect class WeatherDataModelConverters() {
    @TypeConverter
    fun toAlertJson(alerts: List<WeatherEntity.Alert?>?): String?

    @TypeConverter
    fun fromAlertJson(alertString: String): List<WeatherEntity.Alert?>

    @TypeConverter
    fun toDailyWeatherJson(dailyWeatherReports: List<WeatherEntity.Daily?>?): String?

    @TypeConverter
    fun fromDailyWeather(dailyWeatherString: String): List<WeatherEntity.Daily?>

    @TypeConverter
    fun toHourlyWeatherJson(hourlyWeatherReports: List<WeatherEntity.Hourly?>?): String?

    @TypeConverter
    fun fromHourlyWeather(hourlyWeatherString: String): List<WeatherEntity.Hourly?>

    @TypeConverter
    fun toWeatherJson(weatherReports: List<Weather?>?): String?

    @TypeConverter
    fun fromWeatherJson(weatherString: String): List<Weather?>?
}
