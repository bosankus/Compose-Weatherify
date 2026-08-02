package bose.ankush.home.data.wear

import android.content.Context
import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.network.model.WeatherForecast
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Pushes the latest [WeatherForecast] to any paired Wear OS node via the Data Layer API.
 * DataClient persists the item and syncs it to watches opportunistically (including watches
 * that connect later), so this call doesn't need the watch to be reachable right now.
 */
internal class AndroidWeatherWearSync(
    context: Context,
) : WeatherWearSync {
    private val dataClient = Wearable.getDataClient(context)

    override suspend fun sync(
        locationName: String,
        forecast: WeatherForecast,
    ) {
        val request =
            PutDataMapRequest
                .create(WEATHER_PATH)
                .apply {
                    dataMap.putString(KEY_LOCATION_NAME, locationName)
                    dataMap.putString(KEY_FORECAST_JSON, Json.encodeToString(forecast))
                    // Forces a new DataItem version even if the payload is byte-identical to the
                    // last sync, so the watch's listener still fires (DataClient dedupes identical
                    // items).
                    dataMap.putLong(KEY_SYNCED_AT, System.currentTimeMillis())
                }.asPutDataRequest()
                .setUrgent()

        runCatching { dataClient.putDataItem(request).await() }
            .onSuccess { Timber.d("Synced forecast to Wear OS: %s", locationName) }
            .onFailure { Timber.w(it, "Failed to sync forecast to Wear OS") }
    }

    internal companion object {
        const val WEATHER_PATH = "/weather"
        const val KEY_LOCATION_NAME = "location_name"
        const val KEY_FORECAST_JSON = "forecast_json"
        const val KEY_SYNCED_AT = "synced_at"
    }
}
