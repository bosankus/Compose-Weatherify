package bose.ankush.weatherify.data

import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.data.model.WeatherForecast
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
        @Query("APPID") AppId: String = "ad102b2242e9f1c84075385ae4a91116"
    ): CurrentTemperature?

    @GET("/data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("q") location: String = "Kolkata",
        @Query("APPID") AppId: String = "ad102b2242e9f1c84075385ae4a91116"
    ): WeatherForecast?


    /*companion object {
        // certificate pinning
        private val certificatePinner = CertificatePinner.Builder()
            .add(BASE_URL, "092343KANFJDNSFJN498NDJFNF")
            .build()

        private val okHttpInterceptor =
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

        private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(okHttpInterceptor)
            .addNetworkInterceptor(CustomInterceptor())
            .certificatePinner(certificatePinner)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

        fun create(): ApiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }*/
}

/*
class CustomInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val cacheControl = CacheControl.Builder()
            .maxAge(10, TimeUnit.DAYS)
            .build()
        return response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }

}*/
