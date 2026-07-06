package bose.ankush.home.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class HomeUserPreferences(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLocationOverridden: Boolean = false,
    val overrideLat: Double? = null,
    val overrideLon: Double? = null,
    val overrideLocationName: String? = null,
)

internal class HomeWeatherPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    fun getUserPreferencesFlow(): Flow<HomeUserPreferences> =
        dataStore.data.map { preferences ->
            HomeUserPreferences(
                latitude = preferences[Keys.LATITUDE],
                longitude = preferences[Keys.LONGITUDE],
                isLocationOverridden = preferences[Keys.IS_LOCATION_OVERRIDDEN] ?: false,
                overrideLat = preferences[Keys.OVERRIDE_LAT],
                overrideLon = preferences[Keys.OVERRIDE_LON],
                overrideLocationName = preferences[Keys.OVERRIDE_LOCATION_NAME],
            )
        }

    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
        dataStore.edit { preferences ->
            preferences[Keys.LATITUDE] = coordinates.first
            preferences[Keys.LONGITUDE] = coordinates.second
        }
    }

    suspend fun saveLocationOverride(
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

    suspend fun clearLocationOverride() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.OVERRIDE_LAT)
            preferences.remove(Keys.OVERRIDE_LON)
            preferences.remove(Keys.OVERRIDE_LOCATION_NAME)
            preferences[Keys.IS_LOCATION_OVERRIDDEN] = false
        }
    }

    suspend fun clearAll() {
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

    companion object {
        const val FILE_NAME = "home_weather.preferences_pb"
    }
}
