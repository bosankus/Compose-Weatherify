package bose.ankush.weatherify.base.common

import android.annotation.SuppressLint

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

/*General constants*/
const val OPEN_WEATHER_HOSTNAME = "openweathermap.org"
const val OPEN_WEATHER_BASE_URL = "https://api.$OPEN_WEATHER_HOSTNAME/"
const val OPEN_WEATHER_IMG_URL = "https://$OPEN_WEATHER_HOSTNAME/img/wn/"
const val OPEN_WEATHER_CERT_PIN = "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="

const val APP_UPDATE_REQ_CODE = 111

/*Shared Preference Keys*/
const val APP_PREFERENCE_KEY = "app_preferences"

/*Fallback user location coordinates*/
const val DEFAULT_CITY_NAME = "New Delhi"

/* Permission constants */
const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
const val ACCESS_PHONE_CALL = android.Manifest.permission.CALL_PHONE
@SuppressLint("InlinedApi")
const val ACCESS_NOTIFICATION = android.Manifest.permission.POST_NOTIFICATIONS

val PERMISSIONS_TO_REQUEST = arrayOf(
    ACCESS_FINE_LOCATION,
    ACCESS_COARSE_LOCATION
)

/*Room central db name*/
const val WEATHER_DATABASE_NAME = "central_weather_table"
const val AQ_DATABASE_NAME = "central_aq_table"
const val PHONE_NUMBER = "tel:+91XXXXXXXXX"

/*Remote keys*/
const val ENABLE_NOTIFICATION = "enable_notification"
