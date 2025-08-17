package bose.ankush.network.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Android implementation of createPlatformHttpClient
 */
actual fun createPlatformHttpClient(json: Json): HttpClient {
    return HttpClient(Android) {
        engine {
            connectTimeout = 60_000
            socketTimeout = 60_000
        }
        install(ContentNegotiation) {
            json(json)
            // Register for mixed content type (application/json, text/html)
            json(
                json,
                contentType = ContentType.parse("application/json, text/html; charset=UTF-8")
            )
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor Android: $message")
                }
            }
            level = LogLevel.INFO
        }
    }
}
