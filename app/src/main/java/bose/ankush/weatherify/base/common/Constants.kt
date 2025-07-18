package bose.ankush.weatherify.base.common

import android.annotation.SuppressLint

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/
const val WEATHER_IMG_URL = "https://openweathermap.org/img/wn/"

const val APP_UPDATE_REQ_CODE = 111

/*Shared Preference Keys*/
const val APP_PREFERENCE_KEY = "app_preferences"

/*Fallback user location coordinates*/
const val DEFAULT_CITY_NAME = "New Delhi"

/* Permission constants */
const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
@SuppressLint("InlinedApi")
const val ACCESS_NOTIFICATION = android.Manifest.permission.POST_NOTIFICATIONS

val PERMISSIONS_TO_REQUEST = arrayOf(
    ACCESS_FINE_LOCATION,
    ACCESS_COARSE_LOCATION
)

/*Remote keys*/
const val ENABLE_NOTIFICATION = "enable_notification"
