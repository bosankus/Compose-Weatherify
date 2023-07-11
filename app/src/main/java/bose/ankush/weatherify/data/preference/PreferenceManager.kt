package bose.ankush.weatherify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import bose.ankush.weatherify.common.APP_PREFERENCE_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_PREFERENCE_KEY)

    val getLocationPreference: Flow<Pair<Double, Double>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preference ->
            val latitude = preference[PreferenceKeys.USER_LAT_LOCATION] ?: 0.0
            val longitude = preference[PreferenceKeys.USER_LON_LOCATION] ?: 0.0
            // if results are null, returned location will be coordinate 0.0,0.0(https://goo.gl/maps/anJGHft5kPgzKUda9)
            Pair(latitude, longitude)
        }

    suspend fun saveLocationPreferences(latlang: Pair<Double, Double>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_LAT_LOCATION] = latlang.first
            preferences[PreferenceKeys.USER_LON_LOCATION] = latlang.second
        }
    }

    private object PreferenceKeys {
        val USER_LAT_LOCATION = doublePreferencesKey("latitude")
        val USER_LON_LOCATION = doublePreferencesKey("longitude")
    }
}