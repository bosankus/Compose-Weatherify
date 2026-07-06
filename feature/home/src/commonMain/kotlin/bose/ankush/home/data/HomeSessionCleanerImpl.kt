package bose.ankush.home.data

import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.data.preferences.HomeWeatherPreferences
import bose.ankush.home.domain.repository.WeatherRepository

internal class HomeSessionCleanerImpl(
    private val weatherRepository: WeatherRepository,
    private val preferences: HomeWeatherPreferences,
) : HomeSessionCleaner {
    override suspend fun clearOnLogout() {
        weatherRepository.clearAllData()
        preferences.clearAll()
    }
}
