package bose.ankush.weatherify.common

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

/*General constants*/
const val OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
const val DEFAULT_CITY_NAME = "Kolkata"
const val APP_UPDATE_REQ_CODE = 111

/* Permission constants */
/*private const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
private const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
private const val PERMISSION_DENIED = 0
private const val PERMISSION_GIVEN = 1
private const val REQUEST_PERMISSION = 2*/

const val PERMISSION_REQUEST_RATIONAL =
    "You need to accept location permissions to use this app"
const val PERMISSION_REQUEST_CODE = 100

const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
@SuppressLint("InlinedApi") const val BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
const val ANDROID_10 = Build.VERSION_CODES.Q
