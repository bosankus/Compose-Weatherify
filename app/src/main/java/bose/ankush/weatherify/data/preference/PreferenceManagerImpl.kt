package bose.ankush.weatherify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import bose.ankush.weatherify.base.common.APP_PREFERENCE_KEY
import bose.ankush.weatherify.domain.preference.PreferenceManager
import bose.ankush.weatherify.domain.preference.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_PREFERENCE_KEY)

@Singleton
class PreferenceManagerImpl
    @Inject
    constructor(
        @get:ApplicationContext private val context: Context,
    ) : PreferenceManager {

        override fun getUserPreferencesFlow(): Flow<UserPreferences> =
            context.dataStore.data.map { preferences ->
                UserPreferences(
                    latitude = preferences[PreferenceManager.USER_LAT_LOCATION],
                    longitude = preferences[PreferenceManager.USER_LON_LOCATION],
                    isPremium = preferences[PreferenceManager.IS_PREMIUM] ?: false,
                    premiumExpiry = preferences[PreferenceManager.PREMIUM_EXPIRY],
                    isLocationOverridden =
                        preferences[PreferenceManager.IS_LOCATION_OVERRIDDEN]
                            ?: false,
                    overrideLat = preferences[PreferenceManager.OVERRIDE_LAT],
                    overrideLon = preferences[PreferenceManager.OVERRIDE_LON],
                    overrideLocationName = preferences[PreferenceManager.OVERRIDE_LOCATION_NAME],
                )
            }

        override suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
            context.dataStore.edit { preferences ->
                preferences[PreferenceManager.USER_LAT_LOCATION] = coordinates.first
                preferences[PreferenceManager.USER_LON_LOCATION] = coordinates.second
            }
        }

        override suspend fun savePremiumStatus(
            isPremium: Boolean,
            expiryMillis: Long?,
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferenceManager.IS_PREMIUM] = isPremium
                if (expiryMillis != null) {
                    preferences[PreferenceManager.PREMIUM_EXPIRY] = expiryMillis
                } else {
                    preferences.remove(PreferenceManager.PREMIUM_EXPIRY)
                }
            }
        }

        override suspend fun saveLocationOverride(
            lat: Double,
            lon: Double,
            name: String,
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferenceManager.OVERRIDE_LAT] = lat
                preferences[PreferenceManager.OVERRIDE_LON] = lon
                preferences[PreferenceManager.OVERRIDE_LOCATION_NAME] = name
                preferences[PreferenceManager.IS_LOCATION_OVERRIDDEN] = true
            }
        }

        override suspend fun clearLocationOverride() {
            context.dataStore.edit { preferences ->
                preferences.remove(PreferenceManager.OVERRIDE_LAT)
                preferences.remove(PreferenceManager.OVERRIDE_LON)
                preferences.remove(PreferenceManager.OVERRIDE_LOCATION_NAME)
                preferences[PreferenceManager.IS_LOCATION_OVERRIDDEN] = false
            }
        }

        override suspend fun clearAll() {
            context.dataStore.edit { it.clear() }
        }
    }
