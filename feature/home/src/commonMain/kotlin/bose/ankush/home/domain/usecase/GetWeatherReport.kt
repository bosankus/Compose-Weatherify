package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.repository.WeatherRepository

internal class GetWeatherReport(
    private val repository: WeatherRepository,
) {
    operator fun invoke(location: Pair<Double, Double>) = repository.getWeatherReport(location)
}
