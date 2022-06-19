package bose.ankush.weatherify.di

import bose.ankush.weatherify.common.OPEN_WEATHER_BASE_URL
import bose.ankush.weatherify.common.WEATHER_BIT_BASE_URL
import bose.ankush.weatherify.data.remote.OpenWeatherApiService
import bose.ankush.weatherify.data.remote.LoggingInterceptor.logBodyInterceptor
import bose.ankush.weatherify.data.remote.NetworkInterceptor.onlineInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(logBodyInterceptor())
            .addNetworkInterceptor(onlineInterceptor())
            .callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun getConverterFactory(): Converter.Factory {
        return GsonConverterFactory.create()
    }

    @Provides
    fun getOpenWeatherApiService(
        converterFactory: Converter.Factory,
        okHttpClient: OkHttpClient
    ): OpenWeatherApiService {
        return Retrofit.Builder()
            .baseUrl(OPEN_WEATHER_BASE_URL)
            .addConverterFactory(converterFactory)
            .client(okHttpClient)
            .build()
            .create(OpenWeatherApiService::class.java)
    }

    @Provides
    fun getWeatherBitApiService(
        converterFactory: Converter.Factory,
        okHttpClient: OkHttpClient
    ): OpenWeatherApiService {
        return Retrofit.Builder()
            .baseUrl(WEATHER_BIT_BASE_URL)
            .addConverterFactory(converterFactory)
            .client(okHttpClient)
            .build()
            .create(OpenWeatherApiService::class.java)
    }
}