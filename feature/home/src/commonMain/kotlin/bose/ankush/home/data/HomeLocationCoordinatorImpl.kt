package bose.ankush.home.data

import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.data.preferences.HomeWeatherPreferences

internal class HomeLocationCoordinatorImpl(
    private val preferences: HomeWeatherPreferences,
) : HomeLocationCoordinator {
    override suspend fun setDefaultLocation(
        lat: Double,
        lon: Double,
        name: String,
    ) {
        preferences.saveLocationOverride(lat, lon, name)
    }
}
