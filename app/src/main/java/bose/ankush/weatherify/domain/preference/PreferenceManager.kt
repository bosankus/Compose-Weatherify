package bose.ankush.weatherify.domain.preference

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

interface PreferenceManager {
    fun getUserPreferencesFlow(): Flow<UserPreferences>

    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>)

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    )

    suspend fun saveLocationOverride(lat: Double, lon: Double, name: String)

    suspend fun clearLocationOverride()

    /**
     * Must be called on logout so no stale state survives into the next session.
     */
    suspend fun clearAll()

    companion object PreferenceKeys {
        val USER_LAT_LOCATION = doublePreferencesKey("latitude")
        val USER_LON_LOCATION = doublePreferencesKey("longitude")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_EXPIRY = longPreferencesKey("premium_expiry")
        val OVERRIDE_LAT = doublePreferencesKey("override_lat")
        val OVERRIDE_LON = doublePreferencesKey("override_lon")
        val OVERRIDE_LOCATION_NAME = stringPreferencesKey("override_location_name")
        val IS_LOCATION_OVERRIDDEN = booleanPreferencesKey("is_location_overridden")
    }
}

data class UserPreferences(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
    val isLocationOverridden: Boolean = false,
    val overrideLat: Double? = null,
    val overrideLon: Double? = null,
    val overrideLocationName: String? = null,
)
