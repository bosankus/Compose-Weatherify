package bose.ankush.weatherify.base.common

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

/*General constants*/
const val OPEN_WEATHER_HOSTNAME = "openweathermap.org"
const val OPEN_WEATHER_BASE_URL = "https://api.$OPEN_WEATHER_HOSTNAME/"
const val OPEN_WEATHER_CERT_PIN = "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="

const val APP_UPDATE_REQ_CODE = 111

/*Shared Preference Keys*/
const val APP_PREFERENCE_KEY = "app_preferences"

/*Fallback user location coordinates*/
val DEFAULT_LOCATION_COORDINATES = Pair(28.61792, 77.2079)
const val DEFAULT_CITY_NAME = "New Delhi"

/* Permission constants */
private const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
/*private const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
private const val PERMISSION_DENIED = 0
private const val PERMISSION_GIVEN = 1
private const val REQUEST_PERMISSION = 2*/