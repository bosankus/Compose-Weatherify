package bose.ankush.weatherify.data.remote

import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.data.remote.dto.AirQualityDto
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import retrofit2.http.GET
import retrofit2.http.Query

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
interface AirQualityApiService {

    @GET("current/airquality")
    suspend fun getCurrentAirQuality(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("key") AppId: String = BuildConfig.WEATHERBIT_API
    ): AirQualityDto

}
