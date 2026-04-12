package bose.ankush.weatherify.di

import bose.ankush.storage.api.TokenStorage
import bose.ankush.weatherify.base.config.AppConfig
import bose.ankush.weatherify.domain.preference.PreferenceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint that exposes singletons needed to configure the Koin payment module.
 * Used in WeatherifyApplication after Hilt has initialized (i.e. after super.onCreate()).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PaymentKoinBridgeEntryPoint {
    fun tokenStorage(): TokenStorage
    fun preferenceManager(): PreferenceManager
    fun appConfig(): AppConfig
}
