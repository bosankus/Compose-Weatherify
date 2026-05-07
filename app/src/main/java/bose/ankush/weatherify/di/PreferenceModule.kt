package bose.ankush.weatherify.di

import bose.ankush.weatherify.data.preference.PreferenceManagerImpl
import bose.ankush.weatherify.domain.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing preference-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferenceModule {
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindPreferenceManager(preferenceManagerImpl: PreferenceManagerImpl): PreferenceManager
}
