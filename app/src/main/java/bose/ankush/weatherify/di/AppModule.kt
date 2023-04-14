package bose.ankush.weatherify.di

import android.app.Application
import android.content.Context
import bose.ankush.weatherify.base.language.DeviceService
import bose.ankush.weatherify.base.language.LocaleService
import bose.ankush.weatherify.domain.model.Country
import bose.ankush.weatherify.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context =
        application.applicationContext

    @Provides
    @Singleton
    fun provideLocaleService(
        context: Context,
        country: Country,
        deviceService: DeviceService,
        userPreferences: UserPreferences
    ): LocaleService =
        LocaleService(context, country, deviceService, userPreferences)
}