package bose.ankush.network.di

import bose.ankush.analytics.ErrorReporter
import bose.ankush.network.util.NetworkConnectivity
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module

expect fun createPlatformHttpClient(json: Json): HttpClient

expect val networkDomainModule: Module

@Suppress("unused")
fun createHttpClient(
    errorReporter: ErrorReporter,
    networkConnectivity: NetworkConnectivity,
): HttpClient {
    val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
            coerceInputValues = true
        }
    val client = createPlatformHttpClient(json)
    return client.config {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
        // Reporting lives here rather than at each call site so the endpoint is taken from the
        // request Ktor actually sent — it can't drift from the URL the way a hand-written label
        // would. Covers both transport failures (RequestError hook) and response/deserialization
        // failures (ReceiveError hook, which runs before the Receive phase).
        //
        // encodedPath deliberately excludes the query string, so nothing sensitive passed as a
        // query parameter ends up in a crash report.
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, request ->
                // Being offline is a user condition, not a defect. Without this guard a single
                // commute through a tunnel files a non-fatal per request, which burns quota and
                // buries the failures that actually indicate a bug.
                if (networkConnectivity.isNetworkAvailable()) {
                    errorReporter.recordError(
                        cause,
                        "HTTP call failed",
                        mapOf(
                            "endpoint" to request.url.encodedPath,
                            "method" to request.method.value,
                        ),
                    )
                }
            }
        }
    }
}
