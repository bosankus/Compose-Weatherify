package bose.ankush.network.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * iOS implementation of createPlatformHttpClient
 */
actual fun createPlatformHttpClient(json: Json): HttpClient {
    return HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
                setTimeoutInterval(60.0)
            }
        }
        install(ContentNegotiation) {
            // Register standard JSON handling; handle other content types per request if required.
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor iOS: $message")
                }
            }
            level = LogLevel.INFO
        }
    }
}
