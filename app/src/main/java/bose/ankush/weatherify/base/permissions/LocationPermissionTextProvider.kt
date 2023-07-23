package bose.ankush.weatherify.base.permissions

class FineLocationPermissionTextProvider : PermissionTextProvider {

    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you have permanently declined fine location permission. You can go to app permission settings to enable it."
        } else {
            "Precise Location permissions are required to tracking your run path following precise location."
        }
    }
}

class CoarseLocationPermissionTextProvider : PermissionTextProvider {

    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you have permanently declined coarse location permission. You can go to app permission settings to enable it."
        } else {
            "Approximate location permissions are required to show weather & air quality of your approximate location."
        }
    }
}