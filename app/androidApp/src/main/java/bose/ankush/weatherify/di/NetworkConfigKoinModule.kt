package bose.ankush.weatherify.di

import bose.ankush.network.config.NetworkConfig
import bose.ankush.weatherify.BuildConfig
import org.koin.core.module.Module
import org.koin.dsl.module

val appNetworkConfigKoinModule: Module =
    module {
        single<NetworkConfig> {
            object : NetworkConfig {
                override val unsplashAccessKey: String get() = BuildConfig.UNSPLASH_ACCESS_KEY
            }
        }
    }
