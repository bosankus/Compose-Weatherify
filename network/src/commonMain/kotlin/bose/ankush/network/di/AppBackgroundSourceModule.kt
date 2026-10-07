package bose.ankush.network.di

import bose.ankush.network.api.KtorUnsplashApi
import bose.ankush.network.api.UnsplashApi
import bose.ankush.network.config.NetworkConfig
import org.koin.core.module.Module
import org.koin.dsl.module

/** Requires a [NetworkConfig] binding from the host app. */
val appBackgroundSourceModule: Module =
    module {
        single<UnsplashApi> {
            KtorUnsplashApi(
                httpClient = get(),
                accessKey = get<NetworkConfig>().unsplashAccessKey,
            )
        }
    }
