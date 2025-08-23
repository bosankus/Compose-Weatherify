package bose.ankush.network.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
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
            // Register standard JSON handling once; other content types should be handled explicitly per request if needed.
            json(json)
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
