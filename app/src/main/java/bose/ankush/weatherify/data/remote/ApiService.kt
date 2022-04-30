package bose.ankush.weatherify.data.remote

import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.domain.model.Weather
import bose.ankush.weatherify.data.remote.dto.ForecastDto
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
    ): Response<Weather>

    @GET("/data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("q") location: String = "Kolkata",
        @Query("APPID") AppId: String = BuildConfig.APPID
    ): Response<ForecastDto>

    @GET("/data/2.5/forecast")
    suspend fun getWeatherForecastList(
        @Query("q") location: String = "Kolkata",
        @Query("APPID") AppId: String = BuildConfig.APPID
    ): List<ForecastDto.ForecastList>

}
