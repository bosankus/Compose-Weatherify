package bose.ankush.weatherify.di

import bose.ankush.commonui.photo.AccountPhotoCache
import bose.ankush.network.repository.AccountPhotoStore
import coil3.SingletonImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Keeps the account photo in the same Coil caches the avatars read from. */
val appAccountPhotoKoinModule: Module =
    module {
        single<AccountPhotoStore> {
            val cache = AccountPhotoCache(SingletonImageLoader.get(androidContext()))
            object : AccountPhotoStore {
                override suspend fun store(
                    photoUrl: String,
                    bytes: ByteArray,
                ) = cache.store(photoUrl, bytes)

                override fun remove(photoUrl: String) = cache.remove(photoUrl)

                override fun clear() = cache.clear()
            }
        }
    }
