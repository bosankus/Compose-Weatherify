package bose.ankush.storage.common

/**
 * Constants used in the storage module (Room table / DB file names, DataStore file names).
 *
 * [WEATHER_DATABASE_NAME] is historically used both as the weather entity table name and as the
 * on-disk Room database file name on Android — keep it unchanged for cache continuity.
 */
const val WEATHER_DATABASE_NAME = "central_weather_table"
const val AQ_DATABASE_NAME = "central_aq_table"
const val LOCATION_PREFERENCES_FILE_NAME = "home_weather.preferences_pb"
const val PREMIUM_PREFERENCES_FILE_NAME = "app_preferences.preferences_pb"
