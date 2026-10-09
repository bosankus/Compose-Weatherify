package bose.ankush.weatherify.automotive

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class ParkingCarAppService : CarAppService() {

    // Dev only. Replace with a real allowlist before publishing.
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = ParkingSession()
}
