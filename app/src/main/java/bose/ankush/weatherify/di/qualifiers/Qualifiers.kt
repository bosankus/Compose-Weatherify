package bose.ankush.weatherify.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for weather-related dependencies
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WeatherApi

/**
 * Qualifier for device location-related dependencies
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceLocation