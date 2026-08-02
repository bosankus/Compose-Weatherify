package bose.ankush.home.domain.repository

import bose.ankush.network.model.WeatherForecast

/**
 * Pushes a freshly-fetched forecast to a paired Wear OS watch, if any. The watch never talks to
 * the backend itself — it only renders what the phone already fetched and authenticated — so
 * this is a fire-and-forget, best-effort sync with no result to report back.
 */
interface WeatherWearSync {
    suspend fun sync(
        locationName: String,
        forecast: WeatherForecast,
    )
}
