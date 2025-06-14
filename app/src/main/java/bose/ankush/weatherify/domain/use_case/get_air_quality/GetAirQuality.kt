package bose.ankush.weatherify.domain.use_case.get_air_quality

import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAirQuality @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(lat: Double, lang: Double): Flow<AirQuality?> =
        weatherRepository.getAirQualityReport(Pair(lat, lang))
}
