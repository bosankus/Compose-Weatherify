package bose.ankush.weatherify.automotive

import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** What the screen is showing right now. Same idea as an MVI state. */
private sealed interface ParkingUiState {
    data object Loading : ParkingUiState
    data class Content(val spots: List<ParkingSpot>) : ParkingUiState
    data class Error(val message: String) : ParkingUiState
}

class ParkingListScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = ParkingRepository()
    private var state: ParkingUiState = ParkingUiState.Loading

    // Lives as long as this screen. Cancelled in onDestroy so no request outlives it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
        load()
    }

    private fun load() {
        state = ParkingUiState.Loading
        invalidate()
        scope.launch {
            state = try {
                ParkingUiState.Content(repository.nearby(CENTER_LAT, CENTER_LNG))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ParkingUiState.Error("Couldn't load parking. Check the connection and try again.")
            }
            invalidate()   // tells the car host to call onGetTemplate() again
        }
    }

    override fun onGetTemplate(): Template = when (val current = state) {
        is ParkingUiState.Loading ->
            PlaceListMapTemplate.Builder()
                .setTitle("Nearby parking")
                .setLoading(true)
                .build()

        is ParkingUiState.Error ->
            MessageTemplate.Builder(current.message)
                .setTitle("Nearby parking")
                .setIcon(CarIcon.ERROR)
                .addAction(
                    Action.Builder()
                        .setTitle("Retry")
                        .setOnClickListener { load() }
                        .build()
                )
                .build()

        is ParkingUiState.Content ->
            if (current.spots.isEmpty()) {
                MessageTemplate.Builder("No parking found nearby.")
                    .setTitle("Nearby parking")
                    .build()
            } else {
                contentTemplate(current.spots)
            }
    }

    private fun contentTemplate(spots: List<ParkingSpot>): Template {
        // The car host limits how many rows may be shown. Ask it, don't guess.
        val limit = carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)

        val list = ItemList.Builder()
        spots.take(limit).forEach { spot ->
            list.addItem(
                Row.Builder()
                    .setTitle(spot.name)
                    .addText(detailText(spot))
                    .setMetadata(
                        Metadata.Builder()
                            .setPlace(
                                Place.Builder(CarLocation.create(spot.lat, spot.lng))
                                    .setMarker(PlaceMarker.Builder().build())
                                    .build()
                            )
                            .build()
                    )
                    .setOnClickListener {
                        screenManager.push(ParkingDetailScreen(carContext, spot))
                    }
                    .build()
            )
        }

        return PlaceListMapTemplate.Builder()
            .setTitle("Nearby parking")
            .setItemList(list.build())
            .build()
    }

    // The first character is a placeholder. The host replaces it with the
    // formatted distance (for example "0.4 km").
    private fun detailText(spot: ParkingSpot): CharSequence {
        val facts = listOfNotNull(
            spot.capacity?.let { "$it spaces" },
            spot.isPaid?.let { if (it) "Paid" else "Free" },
        ).joinToString(" · ").ifEmpty { "Parking" }

        val text = SpannableString("  ·  $facts")
        text.setSpan(
            DistanceSpan.create(Distance.create(spot.distanceKm, Distance.UNIT_KILOMETERS)),
            0,
            1,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE,
        )
        return text
    }

    private companion object {
        // MG Road, Bangalore. A fixed search centre until we add real location (next step).
        const val CENTER_LAT = 12.9755
        const val CENTER_LNG = 77.6068
    }
}
