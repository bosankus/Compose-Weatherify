package bose.ankush.weatherify.wear.data

import bose.ankush.network.model.WeatherForecast
import bose.ankush.weatherify.wear.data.WeatherSyncStore.PATH
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Holds the most recently synced forecast + location name from the phone over the Wearable
 * Data Layer. [WeatherSyncListenerService] writes to this while the app is running;
 * [bose.ankush.weatherify.wear.presentation.MainActivity] reads an existing (already-synced)
 * DataItem on launch to cover the case where the sync happened before this process was alive.
 */
internal object WeatherSyncStore {
    private val _synced = MutableStateFlow<SyncedWeather?>(null)
    val synced: StateFlow<SyncedWeather?> = _synced

    fun update(synced: SyncedWeather) {
        _synced.value = synced
    }

    /** Decodes a raw [DataItem] at [PATH] into [SyncedWeather], or null if it can't be parsed. */
    fun parse(item: DataItem): SyncedWeather? {
        if (item.uri.path != PATH) {
            Timber.d("Ignoring data item at unexpected path: %s", item.uri.path)
            return null
        }
        val dataMap = DataMapItem.fromDataItem(item).dataMap
        val locationName = dataMap.getString(KEY_LOCATION_NAME)
        val json = dataMap.getString(KEY_FORECAST_JSON)
        if (locationName == null || json == null) {
            Timber.w(
                "Synced data item missing expected keys (locationName present=%s, json present=%s)",
                locationName != null,
                json != null,
            )
            return null
        }
        return runCatching {
            SyncedWeather(
                locationName,
                Json.decodeFromString<WeatherForecast>(json)
            )
        }
            .onFailure { Timber.w(it, "Failed to parse synced forecast") }
            .getOrNull()
    }

    // Must match the path/keys the phone writes in AndroidWeatherWearSync.
    private const val PATH = "/weather"
    private const val KEY_LOCATION_NAME = "location_name"
    private const val KEY_FORECAST_JSON = "forecast_json"
}

internal data class SyncedWeather(
    val locationName: String,
    val forecast: WeatherForecast,
)
