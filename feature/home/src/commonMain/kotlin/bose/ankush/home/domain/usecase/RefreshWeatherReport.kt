package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.repository.WeatherRepository

internal class RefreshWeatherReport(
    private val repository: WeatherRepository,
) {
    suspend operator fun invoke(
        coordinates: Pair<Double, Double>,
        forceRefresh: Boolean = false,
    ) = repository.refreshWeatherData(coordinates, forceRefresh)
}
