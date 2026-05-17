package bose.ankush.weatherify.data.mapper

import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.Weather
import bose.ankush.storage.room.WeatherEntity
import bose.ankush.network.model.AirQuality as NetworkAirQuality
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast

object NetworkToStorageMapper {

    private fun mapWeatherInfo(list: List<NetworkWeatherForecast.Data.WeatherInfo?>?) =
        list?.mapNotNull { info ->
            info?.let {
                Weather(
                    description = it.description,
                    icon = it.icon,
                    id = it.id,
                    main = it.main,
                )
            }
        }

    private fun mapCurrentToEntity(
        current: NetworkWeatherForecast.Data.Current?,
    ) = current?.let {
        WeatherEntity.Current(
            clouds = it.clouds,
            dt = it.dt,
            feels_like = it.feelsLike,
            humidity = it.humidity,
            pressure = it.pressure,
            sunrise = it.sunrise,
            sunset = it.sunset,
            temp = it.temp,
            uvi = it.uvi,
            weather = mapWeatherInfo(it.weather),
            wind_gust = it.windGust,
            wind_speed = it.windSpeed,
        )
    }

    private fun mapDailyToEntity(
        daily: List<NetworkWeatherForecast.Data.Daily?>?,
    ) = daily?.map { item ->
        item?.let {
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
                temp = it.temp?.let { t ->
                    WeatherEntity.Daily.Temp(
                        day = t.day, eve = t.eve, max = t.max,
                        min = t.min, morn = t.morn, night = t.night,
                    )
                },
                uvi = it.uvi,
                weather = mapWeatherInfo(it.weather),
                wind_gust = it.windGust,
                wind_speed = it.windSpeed,
            )
        }
    }

    private fun mapHourlyToEntity(
        hourly: List<NetworkWeatherForecast.Data.Hourly?>?,
    ) = hourly?.map { item ->
        item?.let {
            WeatherEntity.Hourly(
                clouds = it.clouds,
                dt = it.dt,
                feels_like = it.feelsLike,
                humidity = it.humidity,
                temp = it.temp,
                weather = mapWeatherInfo(it.weather),
            )
        }
    }

    private fun mapAlertsToEntity(
        alerts: List<NetworkWeatherForecast.Data.Alert?>?,
    ) = alerts?.mapNotNull { alert ->
        alert?.let {
            WeatherEntity.Alert(
                description = it.description,
                end = it.end,
                event = it.event,
                sender_name = it.senderName,
                start = it.start,
            )
        }
    } ?: emptyList()

    fun mapWeatherToStorageEntity(weatherData: NetworkWeatherForecast): WeatherEntity {
        val data = weatherData.data
        return WeatherEntity(
            id = 0,
            lastUpdated = System.currentTimeMillis(),
            current = mapCurrentToEntity(data?.current),
            daily = mapDailyToEntity(data?.daily),
            hourly = mapHourlyToEntity(data?.hourly),
            alerts = mapAlertsToEntity(data?.alerts),
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
