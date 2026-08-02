package bose.ankush.weatherify.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import timber.log.Timber

/**
 * Registered in the manifest against the DATA_CHANGED action for [WeatherSyncStore.PATH], so
 * Play Services wakes this service up whenever the phone pushes a new forecast — the app
 * doesn't need to be running or have an active DataClient listener at sync time.
 */
internal class WeatherSyncListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Timber.d("onDataChanged fired with %d event(s)", dataEvents.count)
        dataEvents
            .onEach { Timber.d("Data event type=%d path=%s", it.type, it.dataItem.uri.path) }
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .mapNotNull { WeatherSyncStore.parse(it.dataItem) }
            .forEach { synced ->
                WeatherSyncStore.update(synced)
                Timber.d("Received synced forecast from phone: %s", synced.locationName)
            }
        dataEvents.release()
    }
}
