package bose.ankush.weatherify.di

import android.content.Context
import android.content.res.AssetManager
import bose.ankush.weatherify.base.ApplicationAssetLoader
import bose.ankush.weatherify.base.AssetLoader
import bose.ankush.weatherify.base.configuration.ConfigurationLoader
import bose.ankush.weatherify.base.configuration.RawStringConfigurationLoader
import bose.ankush.weatherify.base.language.DeviceService
import bose.ankush.weatherify.domain.model.Country
import bose.ankush.weatherify.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseModule {

    @Provides
    @Singleton
    fun provideAssetManager(context: Context): AssetManager =
        context.assets

    @Provides
    fun provideAssetLoader(
        configurationLoader: ConfigurationLoader,
        rawStringConfigurationLoader: RawStringConfigurationLoader): AssetLoader =
        ApplicationAssetLoader(configurationLoader, rawStringConfigurationLoader)

    @Provides
    fun provideCountryConfig(assetLoader: AssetLoader): Country =
        assetLoader.getCountryConfiguration()

    @Provides
    fun provideDeviceService(): DeviceService =
        DeviceService()

    @Provides
    @Singleton
    fun provideUserPreferences(context: Context): UserPreferences =
        UserPreferences(context)
}