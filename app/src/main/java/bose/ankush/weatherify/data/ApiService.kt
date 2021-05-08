package bose.ankush.weatherify.data

import bose.ankush.weatherify.model.CurrentTemperature
import bose.ankush.weatherify.model.WeatherForecast
import retrofit2.http.GET
import retrofit2.http.Query

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
interface ApiService {

    @GET("/data/2.5/weather")
    suspend fun getCurrentTemperature(
        @Query("q") location: String = "Bengaluru",
        @Query("APPID") AppId: String = "ad102b2242e9f1c84075385ae4a91116"
    ): CurrentTemperature?

    @GET("/data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("q") location: String = "Bengaluru",
        @Query("APPID") AppId: String = "ad102b2242e9f1c84075385ae4a91116"
    ): WeatherForecast?
}