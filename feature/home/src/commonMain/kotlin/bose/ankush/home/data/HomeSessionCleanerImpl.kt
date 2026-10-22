package bose.ankush.home.data

import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.domain.repository.PlaceEventsRepository
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.network.repository.AccountRepository
import bose.ankush.storage.api.LocationPreferencesStorage

internal class HomeSessionCleanerImpl(
    private val weatherRepository: WeatherRepository,
    private val preferences: LocationPreferencesStorage,
    private val accountRepository: AccountRepository,
    private val placeEvents: PlaceEventsRepository,
) : HomeSessionCleaner {
    override suspend fun clearOnLogout() {
        weatherRepository.clearAllData()
        preferences.clearAll()
        // The next account must not see this one's events or resume from its sync checkpoint.
        placeEvents.clearAll()
        // The next account must not see this one's photo, even for a frame.
        accountRepository.clear()
    }
}
