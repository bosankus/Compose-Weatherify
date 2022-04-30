package bose.ankush.weatherify.data.remote

import okhttp3.logging.HttpLoggingInterceptor

object LoggingInterceptor {

    fun logBodyInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
}