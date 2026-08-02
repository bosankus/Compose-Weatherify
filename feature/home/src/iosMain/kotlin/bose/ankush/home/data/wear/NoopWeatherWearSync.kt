package bose.ankush.home.data.wear

import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.network.model.WeatherForecast

/** Wear OS only exists on Android, so there is nothing to sync from iOS. */
internal class NoopWeatherWearSync : WeatherWearSync {
    override suspend fun sync(
        locationName: String,
        forecast: WeatherForecast,
    ) = Unit
}
