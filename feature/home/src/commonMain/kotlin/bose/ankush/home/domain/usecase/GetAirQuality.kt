package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow

internal class GetAirQuality(
    private val weatherRepository: WeatherRepository,
) {
    operator fun invoke(
        lat: Double,
        lon: Double,
    ): Flow<AirQuality?> = weatherRepository.getAirQualityReport(Pair(lat, lon))

    /** Saved air quality. Room keeps one global row and ignores the coordinates. */
    fun cached(): Flow<AirQuality?> = weatherRepository.getAirQualityReport(CACHE_PROBE_LOCATION)

    private companion object {
        /** Unused by the single-row Room table. Only satisfies the repository signature. */
        val CACHE_PROBE_LOCATION = 0.0 to 0.0
    }
}
