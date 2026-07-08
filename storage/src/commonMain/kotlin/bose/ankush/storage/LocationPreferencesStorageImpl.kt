package bose.ankush.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocationPreferencesStorageImpl(private val dataStore: DataStore<Preferences>) :
    LocationPreferencesStorage {

    override fun getLocationPreferencesFlow(): Flow<LocationPreferences> =
        dataStore.data.map { preferences ->
            LocationPreferences(
                latitude = preferences[Keys.LATITUDE],
                longitude = preferences[Keys.LONGITUDE],
                isLocationOverridden = preferences[Keys.IS_LOCATION_OVERRIDDEN] ?: false,
                overrideLat = preferences[Keys.OVERRIDE_LAT],
                overrideLon = preferences[Keys.OVERRIDE_LON],
                overrideLocationName = preferences[Keys.OVERRIDE_LOCATION_NAME],
            )
        }

    override suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
        dataStore.edit { preferences ->
            preferences[Keys.LATITUDE] = coordinates.first
            preferences[Keys.LONGITUDE] = coordinates.second
        }
    }

    override suspend fun saveLocationOverride(
        lat: Double,
        lon: Double,
        name: String,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.OVERRIDE_LAT] = lat
            preferences[Keys.OVERRIDE_LON] = lon
            preferences[Keys.OVERRIDE_LOCATION_NAME] = name
            preferences[Keys.IS_LOCATION_OVERRIDDEN] = true
        }
    }

    override suspend fun clearLocationOverride() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.OVERRIDE_LAT)
            preferences.remove(Keys.OVERRIDE_LON)
            preferences.remove(Keys.OVERRIDE_LOCATION_NAME)
            preferences[Keys.IS_LOCATION_OVERRIDDEN] = false
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val LATITUDE = doublePreferencesKey("home_latitude")
        val LONGITUDE = doublePreferencesKey("home_longitude")
        val OVERRIDE_LAT = doublePreferencesKey("home_override_lat")
        val OVERRIDE_LON = doublePreferencesKey("home_override_lon")
        val OVERRIDE_LOCATION_NAME = stringPreferencesKey("home_override_location_name")
        val IS_LOCATION_OVERRIDDEN = booleanPreferencesKey("home_is_location_overridden")
    }
}
