package bose.ankush.weatherify.domain.preference

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user preferences
 */
interface PreferenceManager {
    /**
     * Get the flow of preferences (location, premium, etc.)
     */
    fun getLocationPreferenceFlow(): Flow<Preferences>

    /**
     * Save location coordinates to preferences
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>)

    /**
     * Save premium subscription status and expiry
     */
    suspend fun savePremiumStatus(isPremium: Boolean, expiryMillis: Long)

    /**
     * Preference keys
     */
    companion object PreferenceKeys {
        val USER_LAT_LOCATION = androidx.datastore.preferences.core.doublePreferencesKey("latitude")
        val USER_LON_LOCATION = androidx.datastore.preferences.core.doublePreferencesKey("longitude")
        val IS_PREMIUM = androidx.datastore.preferences.core.booleanPreferencesKey("is_premium")
        val PREMIUM_EXPIRY =
            androidx.datastore.preferences.core.longPreferencesKey("premium_expiry")
    }
}