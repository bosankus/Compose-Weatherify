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
}
