package bose.ankush.storage.di

import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.WeatherStorageImpl
import bose.ankush.storage.api.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val storageDomainModule: Module =
    module {
        single<TokenStorage> { EncryptedTokenStorageImpl() }
        single<WeatherStorage> { WeatherStorageImpl() }
    }
