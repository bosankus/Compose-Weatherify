package bose.ankush.weatherify.model.network

import okhttp3.logging.HttpLoggingInterceptor

object LoggingInterceptor {

    fun logBodyInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
}