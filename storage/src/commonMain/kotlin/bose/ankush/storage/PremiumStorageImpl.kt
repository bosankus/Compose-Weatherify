package bose.ankush.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import bose.ankush.storage.api.PremiumStatus
import bose.ankush.storage.api.PremiumStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PremiumStorageImpl(
    private val dataStore: DataStore<Preferences>,
) : PremiumStorage {
    override fun getPremiumStatusFlow(): Flow<PremiumStatus> =
        dataStore.data.map { preferences ->
            PremiumStatus(
                isPremium = preferences[Keys.IS_PREMIUM] ?: false,
                premiumExpiry = preferences[Keys.PREMIUM_EXPIRY],
            )
        }

    override suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_PREMIUM] = isPremium
            if (expiryMillis != null) {
                preferences[Keys.PREMIUM_EXPIRY] = expiryMillis
            } else {
                preferences.remove(Keys.PREMIUM_EXPIRY)
            }
        }
    }

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_EXPIRY = longPreferencesKey("premium_expiry")
    }
}
