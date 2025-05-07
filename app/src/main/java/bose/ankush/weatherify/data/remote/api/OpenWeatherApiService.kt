package bose.ankush.weatherify.data.remote.api

import bose.ankush.weatherify.data.room.weather.WeatherEntity
import bose.ankush.weatherify.domain.model.AirQuality
import retrofit2.http.GET
import retrofit2.http.Query

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
interface OpenWeatherApiService {

    @GET("get-air-pollution")
    suspend fun getCurrentAirQuality(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String
    ): AirQuality

    @GET("get-weather")
    suspend fun getOneCallWeather(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String
    ): WeatherEntity
}
