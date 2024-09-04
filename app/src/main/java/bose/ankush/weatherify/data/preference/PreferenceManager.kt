package bose.ankush.weatherify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import bose.ankush.weatherify.base.common.APP_PREFERENCE_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_PREFERENCE_KEY)

    fun getLocationPreferenceFlow(): Flow<Preferences> = context.dataStore.data

    suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAT_LOCATION] = coordinates.first
            preferences[USER_LON_LOCATION] = coordinates.second
        }
    }

    companion object PreferenceKeys {
        val USER_LAT_LOCATION = doublePreferencesKey("latitude")
        val USER_LON_LOCATION = doublePreferencesKey("longitude")
    }
}