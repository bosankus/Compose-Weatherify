package bose.ankush.weatherify.data.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import bose.ankush.weatherify.base.common.DATABASE_NAME

@Entity(tableName = DATABASE_NAME)
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @field:TypeConverters(Converters::class) val alerts: List<Alert?>? = listOf(),
    @Embedded val current: Current? = null,
    @field:TypeConverters(Converters::class) val daily: List<Daily?>? = listOf(),
    @field:TypeConverters(Converters::class) val hourly: List<Hourly?>? = listOf(),
) {
    data class Alert(
        val description: String?,
        val end: Int?,
        val event: String?,
        val sender_name: String?,
        val start: Int?,
    )

    data class Current(
        val clouds: Int?,
        val dt: Long?,
        val feels_like: Double?,
        val humidity: Int?,
        val pressure: Int?,
        val sunrise: Int?,
        val sunset: Int?,
        val temp: Double?,
        val uvi: Double?,
        @field:TypeConverters(Converters::class) val weathers: List<Weather?>? = emptyList(),
        val wind_gust: Double?,
        val wind_speed: Double?
    )

    data class Daily(
        val clouds: Int?,
        val dew_point: Double?,
        val dt: Long?,
        val humidity: Int?,
        val pressure: Int?,
        val rain: Double?,
        val summary: String?,
        val sunrise: Int?,
        val sunset: Int?,
        @Embedded val temp: Temp?,
        val uvi: Double?,
        @field:TypeConverters(Converters::class) val weathers: List<Weather?>? = emptyList(),
        val wind_gust: Double?,
        val wind_speed: Double?
    ) {
        data class Temp(
            val day: Double?,
            val eve: Double?,
            val max: Double?,
            val min: Double?,
            val morn: Double?,
            val night: Double?
        )
    }

    data class Hourly(
        val clouds: Int?,
        val dt: Long?,
        val feels_like: Double?,
        val humidity: Int?,
        val temp: Double?,
        @field:TypeConverters(Converters::class) val weathers: List<Weather?>? = emptyList(),
    )
}

data class Weather(
    val description: String,
    val icon: String,
    val id: Int,
    val main: String
)

