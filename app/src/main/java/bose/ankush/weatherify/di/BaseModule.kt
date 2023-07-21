package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.weatherify.base.preferences.UserPreferences
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
    fun provideUserPreferences(context: Context): UserPreferences =
        UserPreferences(context)
}