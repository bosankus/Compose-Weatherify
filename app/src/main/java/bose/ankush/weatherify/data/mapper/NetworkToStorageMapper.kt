package bose.ankush.weatherify.data.mapper

import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.Weather
import bose.ankush.storage.room.WeatherEntity
import bose.ankush.network.model.AirQuality as NetworkAirQuality
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast

/**
 * Mapper to convert Network layer models to Storage layer entities.
 *
 * This is the network → storage transformation layer.
 * Maps API responses to persistent database models.
 */
object NetworkToStorageMapper {
    /**
     * Maps NetworkWeatherForecast (API model) to WeatherEntity (database model)
     */
    fun mapWeatherToStorageEntity(weatherData: NetworkWeatherForecast): WeatherEntity {
        val data = weatherData.data

        return WeatherEntity(
            id = 0,
            lastUpdated = System.currentTimeMillis(),
            current =
                data?.current?.let { current ->
                    WeatherEntity.Current(
                        clouds = current.clouds,
                        dt = current.dt,
                        feels_like = current.feelsLike,
                        humidity = current.humidity,
                        pressure = current.pressure,
                        sunrise = current.sunrise,
                        sunset = current.sunset,
                        temp = current.temp,
                        uvi = current.uvi,
                        weather =
                            current.weather?.mapNotNull { info ->
                                info?.let {
                                    Weather(
                                        description = it.description,
                                        icon = it.icon,
                                        id = it.id,
                                        main = it.main,
                                    )
                                }
                            },
                        wind_gust = current.windGust,
                        wind_speed = current.windSpeed,
                    )
                },
            daily =
                data?.daily?.map { daily ->
                    daily?.let {
                        WeatherEntity.Daily(
                            clouds = it.clouds,
                            dew_point = it.dewPoint,
                            dt = it.dt,
                            humidity = it.humidity,
                            pressure = it.pressure,
                            rain = it.rain,
                            summary = it.summary,
                            sunrise = it.sunrise,
                            sunset = it.sunset,
                            temp =
                                it.temp?.let { temp ->
                                    WeatherEntity.Daily.Temp(
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
                                it.weather?.mapNotNull { info ->
                                    info?.let {
                                        Weather(
                                            description = it.description,
                                            icon = it.icon,
                                            id = it.id,
                                            main = it.main,
                                        )
                                    }
                                },
                            wind_gust = it.windGust,
                            wind_speed = it.windSpeed,
                        )
                    }
                },
            hourly =
                data?.hourly?.map { hourly ->
                    hourly?.let { it ->
                        WeatherEntity.Hourly(
                            clouds = it.clouds,
                            dt = it.dt,
                            feels_like = it.feelsLike,
                            humidity = it.humidity,
                            temp = it.temp,
                            weather =
                                it.weather?.mapNotNull { info ->
                                    info?.let {
                                        Weather(
                                            description = it.description,
                                            icon = it.icon,
                                            id = it.id,
                                            main = it.main,
                                        )
                                    }
                                },
                        )
                    }
                },
            alerts =
                data?.alerts?.mapNotNull { alert ->
                    alert?.let {
                        WeatherEntity.Alert(
                            description = it.description,
                            end = it.end,
                            event = it.event,
                            sender_name = it.senderName,
                            start = it.start,
                        )
                    }
                } ?: emptyList(),
        )
    }

    /**
     * Maps the air quality data embedded in the unified weather response to AirQualityEntity.
     * When [airQualityData] is null (free tier — air quality not included), stores a default
     * entity so existing storage contracts are preserved.
     */
    fun mapAirQualityToStorageEntity(airQualityData: NetworkAirQuality.Data?): AirQualityEntity {
        val entry = airQualityData?.list?.firstOrNull()
        return AirQualityEntity(
            id = null,
            aqi = entry?.main?.aqi,
            co = entry?.components?.co,
            no2 = entry?.components?.no2,
            o3 = entry?.components?.o3,
            so2 = entry?.components?.so2,
            pm10 = entry?.components?.pm10,
            pm25 = entry?.components?.pm25,
        )
    }
}
