package bose.ankush.weatherify.domain.use_case.get_weather_reports

import bose.ankush.weatherify.domain.repository.WeatherRepository
import javax.inject.Inject

class GetWeatherReport
@Inject
constructor(
    private val repository: WeatherRepository,
) {
    operator fun invoke(location: Pair<Double, Double>) = repository.getWeatherReport(location)
}
