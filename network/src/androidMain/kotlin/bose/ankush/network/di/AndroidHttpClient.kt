package bose.ankush.network.di

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * OkHttp, not Ktor's `Android` engine: that one runs on the platform's old bundled OkHttp, which
 * crashes the app ("Unbalanced enter/exit") when a request is cancelled mid-read — for example an
 * avatar download whose composable leaves the screen. Coil's own `HttpClient()` picks up this
 * engine too, being the only one on the classpath.
 */
actual fun createPlatformHttpClient(json: Json): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger =
                object : Logger {
                    override fun log(message: String) {
                        Log.d("Ktor Android:", message)
                    }
                }
            level = LogLevel.INFO
        }
    }
