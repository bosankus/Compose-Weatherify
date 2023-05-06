package bose.ankush.weatherify.preferences

import android.content.SharedPreferences

abstract class ManagedPreferences {

    /**
     * Get [SharedPreferences] for this class
     */
    abstract fun getSharedPreferences(): SharedPreferences

    /**
     * Store a [Boolean] in [SharedPreferences]
     * @param key: key to identify stored value
     * @param value: value to be stored
     */
    protected fun put(key: String, value: Boolean) {
        getSharedPreferences().edit().putBoolean(key, value).apply()
    }

    /**
     * Get [Boolean] from [SharedPreferences]
     * @param key: key to identify stored value
     * @param default: default value in case [key] does not exists
     */
    protected fun get(key: String, default: Boolean): Boolean =
        getSharedPreferences().getBoolean(key, default)

    /**
     * Store a [String] in [SharedPreferences]
     * @param key: key to identify stored value
     * @param value: value to be stored
     */
    protected fun put(key: String, value: String?) {
        getSharedPreferences().edit().putString(key, value).apply()
    }

    /**
     * Get a nullable [String] from [SharedPreferences]
     * @param key: key to identify stored value
     * @param default: default value in case [key] does not exists
     */
    protected fun get(key: String, default: String): String? =
        getSharedPreferences().getString(key, default)
}