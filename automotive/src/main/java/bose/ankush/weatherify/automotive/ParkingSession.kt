package bose.ankush.weatherify.automotive

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class ParkingSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        ParkingListScreen(carContext)
}