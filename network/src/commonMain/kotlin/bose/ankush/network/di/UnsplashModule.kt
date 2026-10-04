package bose.ankush.network.di

import bose.ankush.network.api.KtorUnsplashApi
import bose.ankush.network.api.UnsplashApi
import org.koin.core.module.Module
import org.koin.dsl.module

fun unsplashKoinModule(accessKey: String): Module =
    module {
        single<UnsplashApi> {
            KtorUnsplashApi(
                httpClient = get(),
                accessKey = accessKey,
            )
        }
    }
