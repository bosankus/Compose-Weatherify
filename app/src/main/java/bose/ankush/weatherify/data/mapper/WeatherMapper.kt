package bose.ankush.weatherify.data.mapper

import bose.ankush.storage.room.WeatherEntity
import bose.ankush.weatherify.domain.model.WeatherCondition
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.storage.room.Weather as StorageWeather

/**
 * Mapper class to convert between WeatherEntity (data layer) and WeatherForecast (domain layer)
 */
object WeatherMapper {
    /**
     * Maps a Storage Weather entity to a WeatherCondition domain model
     */
    private fun mapStorageWeatherToDomain(weather: StorageWeather): WeatherCondition =
        WeatherCondition(
            description = weather.description ?: "",
            icon = weather.icon ?: "",
            id = weather.id,
            main = weather.main ?: "",
        )

    /**
     * Maps a Storage WeatherEntity to a WeatherForecast domain model
     */
    fun mapToDomain(entity: WeatherEntity?): WeatherForecast? {
        if (entity == null) return null

        return WeatherForecast(
            id = entity.id,
            alerts =
                entity.alerts?.map { alert ->
                    alert?.let {
                        WeatherForecast.Alert(
                            description = it.description,
                            end = it.end,
                            event = it.event,
                            sender_name = it.sender_name,
                            start = it.start,
                        )
                    }
                },
            current =
                entity.current?.let { current ->
                    WeatherForecast.Current(
                        clouds = current.clouds,
                        dt = current.dt,
                        feels_like = current.feels_like,
                        humidity = current.humidity,
                        pressure = current.pressure,
                        sunrise = current.sunrise,
                        sunset = current.sunset,
                        temp = current.temp,
                        uvi = current.uvi,
                        weather =
                            current.weather?.map { weather ->
                                weather?.let {
                                    mapStorageWeatherToDomain(it)
                                }
                            },
                        wind_gust = current.wind_gust,
                        wind_speed = current.wind_speed,
                    )
                },
            daily =
                entity.daily?.map { daily ->
                    daily?.let {
                        WeatherForecast.Daily(
                            clouds = it.clouds,
                            dew_point = it.dew_point,
                            dt = it.dt,
                            humidity = it.humidity,
                            pressure = it.pressure,
                            rain = it.rain,
                            summary = it.summary,
                            sunrise = it.sunrise,
                            sunset = it.sunset,
                            temp =
                                it.temp?.let { temp ->
                                    WeatherForecast.Daily.Temp(
                                        day = temp.day,
                                        eve = temp.eve,
                                        max = temp.max,
                                        min = temp.min,
                                        morn = temp.morn,
                                        night = temp.night,
                                    )
                                },
                            uvi = it.uvi,
                            weather =
                                it.weather?.map { weather ->
                                    weather?.let {
                                        mapStorageWeatherToDomain(it)
                                    }
                                },
                            wind_gust = it.wind_gust,
                            wind_speed = it.wind_speed,
                        )
                    }
                },
            hourly =
                entity.hourly?.map { hourly ->
                    hourly?.let {
                        WeatherForecast.Hourly(
                            clouds = it.clouds,
                            dt = it.dt,
                            feels_like = it.feels_like,
                            humidity = it.humidity,
                            temp = it.temp,
                            weather =
                                it.weather?.map { weather ->
                                    weather?.let {
                                        mapStorageWeatherToDomain(it)
                                    }
                                },
                        )
                    }
                },
            lastUpdated = entity.lastUpdated,
        )
    }
}
