package bose.ankush.weatherify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import bose.ankush.weatherify.base.common.APP_PREFERENCE_KEY
import bose.ankush.weatherify.domain.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferenceManager that uses DataStore
 */
@Singleton
class PreferenceManagerImpl @Inject constructor(@get:ApplicationContext private val context: Context) :
    PreferenceManager {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_PREFERENCE_KEY)

    override fun getLocationPreferenceFlow(): Flow<Preferences> = context.dataStore.data

    override suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceManager.USER_LAT_LOCATION] = coordinates.first
            preferences[PreferenceManager.USER_LON_LOCATION] = coordinates.second
        }
    }

    override suspend fun savePremiumStatus(isPremium: Boolean, expiryMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceManager.IS_PREMIUM] = isPremium
            preferences[PreferenceManager.PREMIUM_EXPIRY] = expiryMillis
        }
    }
}
