package bose.ankush.weatherify.base.common

import android.annotation.SuppressLint

const val WEATHER_IMG_URL = "https://openweathermap.org/img/wn/"

const val APP_UPDATE_REQ_CODE = 111

const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION

@SuppressLint("InlinedApi")
const val ACCESS_NOTIFICATION = android.Manifest.permission.POST_NOTIFICATIONS

val PERMISSIONS_TO_REQUEST =
    arrayOf(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION,
    )

const val KELVIN_OFFSET = 273
