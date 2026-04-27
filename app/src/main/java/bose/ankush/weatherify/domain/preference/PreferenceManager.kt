package bose.ankush.weatherify.domain.preference

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user preferences
 */
interface PreferenceManager {
    /**
     * Get the flow of user preferences
     */
    fun getUserPreferencesFlow(): Flow<UserPreferences>

    /**
     * Save location coordinates to preferences
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>)

    /**
     * Save premium subscription status and expiry
     */
    suspend fun savePremiumStatus(isPremium: Boolean, expiryMillis: Long?)

    /**
     * Clear all stored preferences (location, premium status, expiry).
     * Must be called on logout so no stale state survives into the next session.
     */
    suspend fun clearAll()

    /**
     * Preference keys
     */
    companion object PreferenceKeys {
        val USER_LAT_LOCATION = doublePreferencesKey("latitude")
        val USER_LON_LOCATION = doublePreferencesKey("longitude")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_EXPIRY = longPreferencesKey("premium_expiry")
    }
}

/**
 * Data class representing all user-specific preferences.
 */
data class UserPreferences(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null
)
