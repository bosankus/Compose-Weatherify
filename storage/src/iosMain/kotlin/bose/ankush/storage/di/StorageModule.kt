package bose.ankush.storage.di

import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.api.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val storageDomainModule: Module =
    module {
        single<TokenStorage> { EncryptedTokenStorageImpl() }
    }
