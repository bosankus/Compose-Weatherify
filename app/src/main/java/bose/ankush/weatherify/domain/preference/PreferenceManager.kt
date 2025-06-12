package bose.ankush.weatherify.domain.preference

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user preferences
 */
interface PreferenceManager {
    /**
     * Get the flow of location preferences
     */
    fun getLocationPreferenceFlow(): Flow<Preferences>

    /**
     * Save location coordinates to preferences
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>)

    /**
     * Preference keys
     */
    companion object PreferenceKeys {
        val USER_LAT_LOCATION = androidx.datastore.preferences.core.doublePreferencesKey("latitude")
        val USER_LON_LOCATION = androidx.datastore.preferences.core.doublePreferencesKey("longitude")
    }
}