package bose.ankush.weatherify.di

import android.app.Application
import android.content.Context
import bose.ankush.weatherify.base.common.LoggerFactory
import bose.ankush.weatherify.base.common.TimberLoggerFactory
import bose.ankush.weatherify.base.config.AndroidAppConfig
import bose.ankush.weatherify.base.config.AppConfig
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
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideLoggerFactory(): LoggerFactory = TimberLoggerFactory()

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AndroidAppConfig()
}
