package bose.ankush.weatherify.domain.preference

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow

interface PreferenceManager {
    fun getUserPreferencesFlow(): Flow<UserPreferences>

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    )

    companion object PreferenceKeys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_EXPIRY = longPreferencesKey("premium_expiry")
    }
}

data class UserPreferences(
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
)
