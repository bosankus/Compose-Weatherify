package bose.ankush.weatherify.data.mapper

import bose.ankush.network.model.AirQuality as NetworkAirQuality
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast
import bose.ankush.network.model.WeatherCondition as NetworkWeatherCondition
import bose.ankush.weatherify.data.room.weather.AirQualityEntity
import bose.ankush.weatherify.data.room.weather.WeatherEntity
import bose.ankush.weatherify.data.room.weather.Weather as WeatherData
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.weatherify.domain.model.WeatherCondition

/**
 * Mapper class to convert between network models and app models
 */
object NetworkMapper {

    /**
     * Maps a network AirQuality model to an app AirQuality domain model
     */
    fun mapAirQualityToDomain(network: NetworkAirQuality): AirQuality {
        return AirQuality(
            id = network.id,
            aqi = network.aqi,
            co = network.co,
            no2 = network.no2,
            o3 = network.o3,
            so2 = network.so2,
            pm10 = network.pm10,
            pm25 = network.pm25
        )
    }

    /**
     * Maps a network AirQuality model to an app AirQualityEntity
     */
    fun mapAirQualityToEntity(network: NetworkAirQuality): AirQualityEntity {
        return AirQualityEntity(
            id = network.id,
            aqi = network.aqi,
            co = network.co,
            no2 = network.no2,
            o3 = network.o3,
            so2 = network.so2,
            pm10 = network.pm10,
            pm25 = network.pm25
        )
    }

    /**
     * Maps a network WeatherForecast model to an app WeatherEntity
     */
    fun mapWeatherForecastToEntity(network: NetworkWeatherForecast): WeatherEntity {
        return WeatherEntity(
            id = network.id,
            alerts = network.alerts?.map { alert ->
                alert?.let {
                    WeatherEntity.Alert(
                        description = it.description,
                        end = it.end,
                        event = it.event,
                        sender_name = it.sender_name,
                        start = it.start
                    )
                }
            },
            current = network.current?.let { current ->
                WeatherEntity.Current(
                    clouds = current.clouds,
                    dt = current.dt,
                    feels_like = current.feels_like,
                    humidity = current.humidity,
                    pressure = current.pressure,
                    sunrise = current.sunrise,
                    sunset = current.sunset,
                    temp = current.temp,
                    uvi = current.uvi,
                    weather = current.weather?.map { weather ->
                        weather?.let {
                            mapWeatherConditionToWeatherData(it)
                        }
                    },
                    wind_gust = current.wind_gust,
                    wind_speed = current.wind_speed
                )
            },
            daily = network.daily?.map { daily ->
                daily?.let {
                    WeatherEntity.Daily(
                        clouds = it.clouds,
                        dew_point = it.dew_point,
                        dt = it.dt,
                        humidity = it.humidity,
                        pressure = it.pressure,
                        rain = it.rain,
                        summary = it.summary,
                        sunrise = it.sunrise,
                        sunset = it.sunset,
                        temp = it.temp?.let { temp ->
                            WeatherEntity.Daily.Temp(
                                day = temp.day,
                                eve = temp.eve,
                                max = temp.max,
                                min = temp.min,
                                morn = temp.morn,
                                night = temp.night
                            )
                        },
                        uvi = it.uvi,
                        weather = it.weather?.map { weather ->
                            weather?.let {
                                mapWeatherConditionToWeatherData(it)
                            }
                        },
                        wind_gust = it.wind_gust,
                        wind_speed = it.wind_speed
                    )
                }
            },
            hourly = network.hourly?.map { hourly ->
                hourly?.let {
                    WeatherEntity.Hourly(
                        clouds = it.clouds,
                        dt = it.dt,
                        feels_like = it.feels_like,
                        humidity = it.humidity,
                        temp = it.temp,
                        weather = it.weather?.map { weather ->
                            weather?.let {
                                mapWeatherConditionToWeatherData(it)
                            }
                        }
                    )
                }
            },
            lastUpdated = network.lastUpdated
        )
    }

    /**
     * Maps a network WeatherCondition to an app Weather data model
     */
    private fun mapWeatherConditionToWeatherData(network: NetworkWeatherCondition): WeatherData {
        return WeatherData(
            description = network.description,
            icon = network.icon,
            id = network.id,
            main = network.main
        )
    }
}