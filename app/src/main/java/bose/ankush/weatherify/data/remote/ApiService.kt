package bose.ankush.weatherify.data.remote

import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import retrofit2.http.GET
import retrofit2.http.Query

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
interface ApiService {

    @GET("/data/2.5/weather")
    suspend fun getTodaysWeatherReport(
        @Query("q") location: String,
        @Query("APPID") AppId: String = BuildConfig.APPID
    ): WeatherDto

    @GET("/data/2.5/forecast")
    suspend fun getWeatherForecastList(
        @Query("q") location: String,
        @Query("APPID") AppId: String = BuildConfig.APPID
    ): ForecastDto

}
