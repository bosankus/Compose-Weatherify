package bose.ankush.storage

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.StoredWeather
import bose.ankush.storage.model.WeatherCondition
import bose.ankush.storage.model.WeatherData
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.Weather
import bose.ankush.storage.room.WeatherDao
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed [WeatherStorage] shared by Android and iOS. The saved row carries its own fetch
 * time, coordinates, and place name, so freshness is read from the row itself.
 */
class WeatherStorageImpl(
    private val weatherDao: WeatherDao,
) : WeatherStorage {
    /**
     * The weather table only signals a change. [WeatherDao.refreshWeather] and
     * [WeatherDao.clearAll] always touch both tables, so the pair is then read in one transaction.
     */
    override fun observeWeather(): Flow<StoredWeather?> {
        return weatherDao.getWeather().map {
            weatherDao.findWeatherWithAirQuality()?.let { (weather, airQuality) ->
                StoredWeather(weather.toWeatherData(), airQuality?.toAirQualityData())
            }
        }
    }

    override suspend fun saveWeatherData(
        weatherData: WeatherData,
        airQualityData: AirQualityData,
    ) {
        withContext(Dispatchers.IO) {
            weatherDao.refreshWeather(
                weatherData.toWeatherEntity(),
                airQualityData.toAirQualityEntity(),
            )
        }
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            weatherDao.clearAll()
        }
    }
}

internal fun List<Weather?>?.toWeatherConditions() =
    this?.map {
        it?.let { w -> WeatherCondition(w.description, w.icon, w.id, w.main) }
    }

internal fun List<WeatherCondition?>?.toStorageWeather() =
    this?.map {
        it?.let { w -> Weather(w.description, w.icon, w.id, w.main) }
    }

internal fun WeatherEntity.toWeatherData() =
    WeatherData(
        id = id,
        alerts =
            alerts?.map {
                it?.let { a ->
                    WeatherData.Alert(a.description, a.end, a.event, a.sender_name, a.start)
                }
            },
        current =
            current?.let {
                WeatherData.Current(
                    clouds = it.clouds,
                    dt = it.dt,
                    feels_like = it.feels_like,
                    humidity = it.humidity,
                    pressure = it.pressure,
                    sunrise = it.sunrise,
                    sunset = it.sunset,
                    temp = it.temp,
                    uvi = it.uvi,
                    weather = it.weather.toWeatherConditions(),
                    wind_gust = it.wind_gust,
                    wind_speed = it.wind_speed,
                )
            },
        daily =
            daily?.map { item ->
                item?.let {
                    WeatherData.Daily(
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
                                WeatherData.Daily.Temp(
                                    t.day,
                                    t.eve,
                                    t.max,
                                    t.min,
                                    t.morn,
                                    t.night,
                                )
                            },
                        uvi = it.uvi,
                        weather = it.weather.toWeatherConditions(),
                        wind_gust = it.wind_gust,
                        wind_speed = it.wind_speed,
                    )
                }
            },
        hourly =
            hourly?.map { item ->
                item?.let {
                    WeatherData.Hourly(
                        clouds = it.clouds,
                        dt = it.dt,
                        feels_like = it.feels_like,
                        humidity = it.humidity,
                        temp = it.temp,
                        weather = it.weather.toWeatherConditions(),
                    )
                }
            },
        lastUpdated = lastUpdated,
        timezoneOffset = timezoneOffset,
        latitude = latitude,
        longitude = longitude,
        placeName = placeName,
    )

internal fun WeatherData.toWeatherEntity() =
    WeatherEntity(
        id = id,
        alerts =
            alerts?.map {
                it?.let { a ->
                    WeatherEntity.Alert(a.description, a.end, a.event, a.sender_name, a.start)
                }
            },
        current =
            current?.let {
                WeatherEntity.Current(
                    clouds = it.clouds,
                    dt = it.dt,
                    feels_like = it.feels_like,
                    humidity = it.humidity,
                    pressure = it.pressure,
                    sunrise = it.sunrise,
                    sunset = it.sunset,
                    temp = it.temp,
                    uvi = it.uvi,
                    weather = it.weather.toStorageWeather(),
                    wind_gust = it.wind_gust,
                    wind_speed = it.wind_speed,
                )
            },
        daily =
            daily?.map { item ->
                item?.let {
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
                        temp =
                            it.temp?.let { t ->
                                WeatherEntity.Daily.Temp(
                                    t.day,
                                    t.eve,
                                    t.max,
                                    t.min,
                                    t.morn,
                                    t.night,
                                )
                            },
                        uvi = it.uvi,
                        weather = it.weather.toStorageWeather(),
                        wind_gust = it.wind_gust,
                        wind_speed = it.wind_speed,
                    )
                }
            },
        hourly =
            hourly?.map { item ->
                item?.let {
                    WeatherEntity.Hourly(
                        clouds = it.clouds,
                        dt = it.dt,
                        feels_like = it.feels_like,
                        humidity = it.humidity,
                        temp = it.temp,
                        weather = it.weather.toStorageWeather(),
                    )
                }
            },
        lastUpdated = lastUpdated,
        timezoneOffset = timezoneOffset,
        latitude = latitude,
        longitude = longitude,
        placeName = placeName,
    )

internal fun AirQualityEntity.toAirQualityData() = AirQualityData(id, aqi, co, no2, o3, so2, pm10, pm25)

internal fun AirQualityData.toAirQualityEntity() = AirQualityEntity(id, aqi, co, no2, o3, so2, pm10, pm25)
