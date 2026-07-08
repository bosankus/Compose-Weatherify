package bose.ankush.home.data

import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.storage.api.LocationPreferencesStorage

internal class HomeSessionCleanerImpl(
    private val weatherRepository: WeatherRepository,
    private val preferences: LocationPreferencesStorage,
) : HomeSessionCleaner {
    override suspend fun clearOnLogout() {
        weatherRepository.clearAllData()
        preferences.clearAll()
    }
}
