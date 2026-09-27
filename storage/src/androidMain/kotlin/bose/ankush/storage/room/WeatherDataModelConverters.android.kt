package bose.ankush.storage.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@ProvidedTypeConverter
actual class WeatherDataModelConverters actual constructor() {
    private val parser: Parser = JsonParser(Gson())

    @TypeConverter
    actual fun toAlertJson(alerts: List<WeatherEntity.Alert?>?): String? =
        parser.toJson(
            alerts,
            object : TypeToken<List<WeatherEntity.Alert?>?>() {}.type,
        )

    @TypeConverter
    actual fun fromAlertJson(alertString: String): List<WeatherEntity.Alert?> =
        parser.fromJson(
            alertString,
            object : TypeToken<List<WeatherEntity.Alert?>?>() {}.type,
        ) ?: emptyList()

    @TypeConverter
    actual fun toDailyWeatherJson(dailyWeatherReports: List<WeatherEntity.Daily?>?): String? =
        parser.toJson(
            dailyWeatherReports,
            object : TypeToken<List<WeatherEntity.Daily?>?>() {}.type,
        )

    @TypeConverter
    actual fun fromDailyWeather(dailyWeatherString: String): List<WeatherEntity.Daily?> =
        parser.fromJson(
            dailyWeatherString,
            object : TypeToken<List<WeatherEntity.Daily?>?>() {}.type,
        ) ?: emptyList()

    @TypeConverter
    actual fun toHourlyWeatherJson(hourlyWeatherReports: List<WeatherEntity.Hourly?>?): String? =
        parser.toJson(
            hourlyWeatherReports,
            object : TypeToken<List<WeatherEntity.Hourly?>?>() {}.type,
        )

    @TypeConverter
    actual fun fromHourlyWeather(hourlyWeatherString: String): List<WeatherEntity.Hourly?> =
        parser.fromJson(
            hourlyWeatherString,
            object : TypeToken<List<WeatherEntity.Hourly?>?>() {}.type,
        ) ?: emptyList()

    @TypeConverter
    actual fun toWeatherJson(weatherReports: List<Weather?>?): String? =
        parser.toJson(
            weatherReports,
            object : TypeToken<List<Weather?>?>() {}.type,
        )

    @TypeConverter
    actual fun fromWeatherJson(weatherString: String): List<Weather?>? =
        parser.fromJson(
            weatherString,
            object : TypeToken<List<Weather?>?>() {}.type,
        )
}
