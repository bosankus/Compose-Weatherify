package bose.ankush.weatherify.di

import bose.ankush.weatherify.base.common.WEATHER_BASE_URL
import bose.ankush.weatherify.data.remote.LoggingInterceptor.logBodyInterceptor
import bose.ankush.weatherify.data.remote.NetworkInterceptor.onlineInterceptor
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.di.qualifiers.WeatherApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    fun providesGson(): Gson {
        return GsonBuilder().create()
    }

    @Singleton
    @Provides
    fun providesOkHttpClient(): OkHttpClient {
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
    fun providesConverterFactory(): Converter.Factory {
        return GsonConverterFactory.create()
    }

    @Singleton
    @Provides
    @WeatherApi
    fun providesOpenWeatherApiService(
        converterFactory: Converter.Factory,
        okHttpClient: OkHttpClient
    ): OpenWeatherApiService {
        return Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(converterFactory)
            .client(okHttpClient)
            .build()
            .create(OpenWeatherApiService::class.java)
    }
}
