package bose.ankush.weatherify.base.common

const val APP_UPDATE_REQ_CODE = 111

const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION

val PERMISSIONS_TO_REQUEST =
    arrayOf(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION,
    )
