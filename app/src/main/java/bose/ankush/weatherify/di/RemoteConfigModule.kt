package bose.ankush.weatherify.di

import bose.ankush.weatherify.data.remote_config.FirebaseRemoteConfigService
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing remote configuration related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindRemoteConfigService(firebaseRemoteConfigService: FirebaseRemoteConfigService): RemoteConfigService
}
