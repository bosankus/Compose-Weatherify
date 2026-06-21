package bose.ankush.weatherify.base.location

/**
 * Platform-agnostic location permission string constants.
 * Avoids importing android.Manifest in shared/common ViewModel code.
 */
object LocationPermissions {
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
}
