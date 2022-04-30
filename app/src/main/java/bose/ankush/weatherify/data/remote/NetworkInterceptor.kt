package bose.ankush.weatherify.data.remote

import okhttp3.Interceptor
import okhttp3.Response

object NetworkInterceptor {

    fun onlineInterceptor(): Interceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        val maxAge = 90 // read from cache for 60 seconds even if there is internet connection
        response.newBuilder()
            .header("Cache-Control", "public, max-age=$maxAge")
            .removeHeader("Pragma")
            .build()
    }
}
