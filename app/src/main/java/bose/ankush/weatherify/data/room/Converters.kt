package bose.ankush.weatherify.data.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken

@ProvidedTypeConverter
class Converters (private val parser: Parser) {

    @TypeConverter
    fun toAlertJson(alerts: List<WeatherEntity.Alert?>?): String = parser.toJson(
        alerts,
        object : TypeToken<List<WeatherEntity.Alert?>?>() {}.type
    ) ?: "[]"

    @TypeConverter
    fun fromAlertJson(alertString: String): List<WeatherEntity.Alert?>? =
        parser.fromJson(
            alertString,
            object : TypeToken<List<WeatherEntity.Alert?>?>() {}.type
        ) ?: emptyList()

    @TypeConverter
    fun toDailyWeatherJson(dailyWeatherReports: List<WeatherEntity.Daily?>?): String = parser.toJson(
        dailyWeatherReports,
        object : TypeToken<List<WeatherEntity.Daily?>?>() {}.type
    ) ?: "[]"

    @TypeConverter
    fun fromDailyWeather(dailyWeatherString: String): List<WeatherEntity.Daily?>? =
        parser.fromJson(
            dailyWeatherString,
            object : TypeToken<List<WeatherEntity.Daily?>?>() {}.type
        ) ?: emptyList()

    @TypeConverter
    fun toHourlyWeatherJson(hourlyWeatherReports: List<WeatherEntity.Hourly?>?): String =
        parser.toJson(
            hourlyWeatherReports,
            object : TypeToken<List<WeatherEntity.Hourly?>?>() {}.type
        ) ?: "[]"

    @TypeConverter
    fun fromHourlyWeather(hourlyWeatherString: String): List<WeatherEntity.Hourly?>? =
        parser.fromJson(
            hourlyWeatherString,
            object : TypeToken<List<WeatherEntity.Hourly?>?>() {}.type
        ) ?: emptyList()

    @TypeConverter
    fun toWeatherJson(weatherReports: List<Weather?>?): String = parser.toJson(
        weatherReports,
        object : TypeToken<List<Weather?>?>() {}.type
    ) ?: "[]"

    @TypeConverter
    fun fromWeatherJson(weatherString: String): List<Weather?>? =
        parser.fromJson(
            weatherString,
            object : TypeToken<List<Weather?>?>() {}.type
        ) ?: emptyList()
}
