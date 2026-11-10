package bose.ankush.home.domain.model

import bose.ankush.home.domain.nearby.GeoPoint

/**
 * The saved forecast, the air quality that came with it, and the place it is for. All arrive
 * from one refresh and are saved in one Room transaction, so they are read as one value.
 * [airQuality] is null when the response had none (free tier).
 */
internal data class WeatherSnapshot(
    val forecast: WeatherForecast,
    val airQuality: AirQuality?,
    /** Where it was fetched. Null for rows saved before schema 6. */
    val point: GeoPoint? = null,
    /** The pinned name, else the reverse-geocoded one, at fetch time. Null when unknown. */
    val placeName: String? = null,
)

/** What a successful [bose.ankush.home.domain.repository.WeatherRepository.refresh] did. */
internal enum class RefreshOutcome {
    /** The network answered and the saved snapshot was replaced. */
    Updated,

    /** The saved snapshot is under the staleness limit, so the network was not asked. */
    UpToDate,
}
