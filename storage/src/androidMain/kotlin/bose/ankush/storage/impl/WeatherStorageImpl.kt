package bose.ankush.storage.impl

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.WeatherCondition
import bose.ankush.storage.model.WeatherData
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.Weather
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WeatherStorageImpl(
    private val weatherDatabase: WeatherDatabase,
) : WeatherStorage {
    // In-memory per-location timestamp map. Keyed by "lat_lon" string.
    // Reset on process restart intentionally — fresh data should be fetched after a cold start.
    private val locationTimestamps = mutableMapOf<String, Long>()
    private val timestampsMutex = Mutex()

    private fun locationKey(coordinates: Pair<Double, Double>) =
        "${coordinates.first}_${coordinates.second}"

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherData?> =
        weatherDatabase.weatherDao().getWeather().map { it?.toWeatherData() }

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQualityData?> =
        weatherDatabase.weatherDao().getAirQuality().map { it?.toAirQualityData() }

    override suspend fun getLastWeatherUpdateTime(coordinates: Pair<Double, Double>): Long =
        timestampsMutex.withLock { locationTimestamps[locationKey(coordinates)] ?: 0L }

    override suspend fun saveLastWeatherUpdateTime(
        coordinates: Pair<Double, Double>,
        time: Long,
    ) {
        timestampsMutex.withLock { locationTimestamps[locationKey(coordinates)] = time }
    }

    override suspend fun saveWeatherData(
        weatherData: WeatherData,
        airQualityData: AirQualityData,
    ) {
        withContext(Dispatchers.IO) {
            weatherDatabase.weatherDao().refreshWeather(
                weatherData.toWeatherEntity(),
                airQualityData.toAirQualityEntity(),
            )
        }
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            weatherDatabase.weatherDao().clearAll()
        }
        timestampsMutex.withLock { locationTimestamps.clear() }
    }

    private fun List<Weather?>?.toWeatherConditions() =
        this?.map { it?.let { w -> WeatherCondition(w.description, w.icon, w.id, w.main) } }

    private fun List<WeatherCondition?>?.toStorageWeather() =
        this?.map { it?.let { w -> Weather(w.description, w.icon, w.id, w.main) } }

    private fun WeatherEntity.toWeatherData() =
        WeatherData(
            id = id,
            alerts = alerts?.map {
                it?.let { a ->
                    WeatherData.Alert(a.description, a.end, a.event, a.sender_name, a.start)
                }
            },
            current = current?.let {
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
            daily = daily?.map { item ->
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
                        temp = it.temp?.let { t ->
                            WeatherData.Daily.Temp(t.day, t.eve, t.max, t.min, t.morn, t.night)
                        },
                        uvi = it.uvi,
                        weather = it.weather.toWeatherConditions(),
                        wind_gust = it.wind_gust,
                        wind_speed = it.wind_speed,
                    )
                }
            },
            hourly = hourly?.map { item ->
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
        )

    private fun WeatherData.toWeatherEntity() =
        WeatherEntity(
            id = id,
            alerts = alerts?.map {
                it?.let { a ->
                    WeatherEntity.Alert(a.description, a.end, a.event, a.sender_name, a.start)
                }
            },
            current = current?.let {
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
            daily = daily?.map { item ->
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
                        temp = it.temp?.let { t ->
                            WeatherEntity.Daily.Temp(t.day, t.eve, t.max, t.min, t.morn, t.night)
                        },
                        uvi = it.uvi,
                        weather = it.weather.toStorageWeather(),
                        wind_gust = it.wind_gust,
                        wind_speed = it.wind_speed,
                    )
                }
            },
            hourly = hourly?.map { item ->
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
        )

    private fun AirQualityEntity.toAirQualityData() =
        AirQualityData(id, aqi, co, no2, o3, so2, pm10, pm25)

    private fun AirQualityData.toAirQualityEntity() =
        AirQualityEntity(id, aqi, co, no2, o3, so2, pm10, pm25)
}
