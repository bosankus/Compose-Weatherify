package bose.ankush.weatherify.data.mapper

import bose.ankush.storage.model.WeatherData
import bose.ankush.weatherify.domain.model.WeatherCondition
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.storage.model.WeatherCondition as StorageWeather

object WeatherMapper {
    private fun mapStorageWeatherToDomain(weather: StorageWeather): WeatherCondition =
        WeatherCondition(
            description = weather.description ?: "",
            icon = weather.icon ?: "",
            id = weather.id,
            main = weather.main ?: "",
        )

    private fun mapWeather(list: List<StorageWeather?>?) = list?.map { it?.let { w -> mapStorageWeatherToDomain(w) } }

    private fun mapAlerts(alerts: List<WeatherData.Alert?>?) =
        alerts?.map { alert ->
            alert?.let {
                WeatherForecast.Alert(
                    description = it.description,
                    end = it.end,
                    event = it.event,
                    sender_name = it.sender_name,
                    start = it.start,
                )
            }
        }

    private fun mapCurrent(current: WeatherData.Current?) =
        current?.let {
            WeatherForecast.Current(
                clouds = it.clouds,
                dt = it.dt,
                feels_like = it.feels_like,
                humidity = it.humidity,
                pressure = it.pressure,
                sunrise = it.sunrise,
                sunset = it.sunset,
                temp = it.temp,
                uvi = it.uvi,
                weather = mapWeather(it.weather),
                wind_gust = it.wind_gust,
                wind_speed = it.wind_speed,
            )
        }

    private fun mapDaily(daily: List<WeatherData.Daily?>?) =
        daily?.map { item ->
            item?.let {
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
                        it.temp?.let { t ->
                            WeatherForecast.Daily.Temp(
                                day = t.day,
                                eve = t.eve,
                                max = t.max,
                                min = t.min,
                                morn = t.morn,
                                night = t.night,
                            )
                        },
                    uvi = it.uvi,
                    weather = mapWeather(it.weather),
                    wind_gust = it.wind_gust,
                    wind_speed = it.wind_speed,
                )
            }
        }

    private fun mapHourly(hourly: List<WeatherData.Hourly?>?) =
        hourly?.map { item ->
            item?.let {
                WeatherForecast.Hourly(
                    clouds = it.clouds,
                    dt = it.dt,
                    feels_like = it.feels_like,
                    humidity = it.humidity,
                    temp = it.temp,
                    weather = mapWeather(it.weather),
                )
            }
        }

    fun mapToDomain(data: WeatherData?): WeatherForecast? {
        if (data == null) return null
        return WeatherForecast(
            id = data.id,
            alerts = mapAlerts(data.alerts),
            current = mapCurrent(data.current),
            daily = mapDaily(data.daily),
            hourly = mapHourly(data.hourly),
            lastUpdated = data.lastUpdated,
        )
    }
}
