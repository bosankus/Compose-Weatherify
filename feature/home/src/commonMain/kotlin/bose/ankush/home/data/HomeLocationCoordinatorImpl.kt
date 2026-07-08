package bose.ankush.home.data

import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.storage.api.LocationPreferencesStorage

internal class HomeLocationCoordinatorImpl(
    private val preferences: LocationPreferencesStorage,
) : HomeLocationCoordinator {
    override suspend fun setDefaultLocation(
        lat: Double,
        lon: Double,
        name: String,
    ) {
        preferences.saveLocationOverride(lat, lon, name)
    }
}
