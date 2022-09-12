package bose.ankush.weatherify.data.remote.api

import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import retrofit2.http.GET
import retrofit2.http.Query

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
interface OpenWeatherApiService {

    @GET("weather")
    suspend fun getTodaysWeatherReport(
        @Query("q") location: String,
        @Query("APPID") AppId: String = BuildConfig.OPEN_WEATHER_API
    ): WeatherDto

    @GET("forecast")
    suspend fun getWeatherForecastList(
        @Query("q") location: String,
        @Query("APPID") AppId: String = BuildConfig.OPEN_WEATHER_API
    ): ForecastDto

}
