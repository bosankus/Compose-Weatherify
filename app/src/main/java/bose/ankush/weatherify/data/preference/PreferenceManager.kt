package bose.ankush.weatherify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import bose.ankush.weatherify.base.common.APP_PREFERENCE_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_PREFERENCE_KEY)

    suspend fun getLocationPreference(callback: LocationPreferenceCallback) {
        try {
            val result = context.dataStore.data.first()
            val latitude = result[PreferenceKeys.USER_LAT_LOCATION] ?: 0.0
            val longitude = result[PreferenceKeys.USER_LON_LOCATION] ?: 0.0
            val coordinate = Pair(latitude, longitude)
            callback.onLocationPreferenceSuccess(coordinate)
        } catch (e: Exception) {
            callback.onLocationPreferenceError(e)
        }
    }

    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_LAT_LOCATION] = coordinates.first
            preferences[PreferenceKeys.USER_LON_LOCATION] = coordinates.second
        }
    }

    private object PreferenceKeys {
        val USER_LAT_LOCATION = doublePreferencesKey("latitude")
        val USER_LON_LOCATION = doublePreferencesKey("longitude")
    }

    interface LocationPreferenceCallback {
        fun onLocationPreferenceSuccess(locationPreference: Pair<Double, Double>)
        fun onLocationPreferenceError(exception: Exception)
    }
}