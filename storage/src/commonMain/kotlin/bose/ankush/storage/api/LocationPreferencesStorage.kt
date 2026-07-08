package bose.ankush.storage.api

import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.flow.Flow

interface LocationPreferencesStorage {
    fun getLocationPreferencesFlow(): Flow<LocationPreferences>

    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>)

    suspend fun saveLocationOverride(
        lat: Double,
        lon: Double,
        name: String,
    )

    suspend fun clearLocationOverride()

    suspend fun clearAll()
}
