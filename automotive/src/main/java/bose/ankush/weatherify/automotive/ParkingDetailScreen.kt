package bose.ankush.weatherify.automotive

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.HostException
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import java.util.Locale

/**
 * Detail view for one parking spot.
 *
 * Pushed on top of [ParkingListScreen] with screenManager.push(...). The car's back
 * button pops it automatically, so no code is needed to go back.
 */
class ParkingDetailScreen(
    carContext: CarContext,
    private val spot: ParkingSpot,
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val capacity = spot.capacity?.let { "$it spaces" } ?: "Not listed"
        val fee = when (spot.isPaid) {
            true -> "Paid parking"
            false -> "Free parking"
            null -> "Not listed"
        }

        val pane = Pane.Builder()
            .addRow(Row.Builder().setTitle("Capacity").addText(capacity).build())
            .addRow(Row.Builder().setTitle("Fee").addText(fee).build())
            .addRow(
                Row.Builder()
                    .setTitle("Distance")
                    .addText(String.format(Locale.getDefault(), "%.1f km away", spot.distanceKm))
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Navigate")
                    .setOnClickListener { navigateToSpot() }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle(spot.name)
            .setHeaderAction(Action.BACK)
            .build()
    }

    /** Hands the location to whichever navigation app the car has, if any. */
    private fun navigateToSpot() {
        val intent = Intent(
            CarContext.ACTION_NAVIGATE,
            Uri.parse("geo:${spot.lat},${spot.lng}"),
        )
        try {
            carContext.startCarApp(intent)
        } catch (e: HostException) {
            CarToast.makeText(carContext, "No navigation app available", CarToast.LENGTH_LONG)
                .show()
        }
    }
}
