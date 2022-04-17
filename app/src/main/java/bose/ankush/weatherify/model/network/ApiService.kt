package bose.ankush.weatherify.model.network

import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.model.model.CurrentTemperature
import bose.ankush.weatherify.model.model.WeatherForecast
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
interface ApiService {

    @GET("/data/2.5/weather")
    suspend fun getCurrentTemperature(
        @Query("q") location: String = "Kolkata",
        @Query("APPID") AppId: String = BuildConfig.APPID
    ): Response<CurrentTemperature>

    @GET("/data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("q") location: String = "Kolkata",
        @Query("APPID") AppId: String = BuildConfig.APPID
    ): Response<WeatherForecast>
}
