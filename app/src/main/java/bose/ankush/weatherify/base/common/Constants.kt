package bose.ankush.weatherify.base.common

import android.annotation.SuppressLint

const val WEATHER_IMG_URL = "https://openweathermap.org/img/wn/"

const val APP_UPDATE_REQ_CODE = 111

const val APP_PREFERENCE_KEY = "app_preferences"

const val DEFAULT_CITY_NAME = "New Delhi"

const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION

@SuppressLint("InlinedApi")
const val ACCESS_NOTIFICATION = android.Manifest.permission.POST_NOTIFICATIONS

val PERMISSIONS_TO_REQUEST =
    arrayOf(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION,
    )

const val ENABLE_NOTIFICATION = "enable_notification"

const val KELVIN_OFFSET = 273

const val LUMINANCE_THRESHOLD = 0.5f

const val AQI_SINGLE_DIGIT_MAX = 9
