package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.commonui.photo.AccountPhotoCache
import bose.ankush.commonui.photo.AccountPhotoKeyMemory
import bose.ankush.network.repository.AccountPhotoStore
import coil3.SingletonImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Keeps the account photo in the same Coil caches the avatars read from. */
val appAccountPhotoKoinModule: Module =
    module {
        single<AccountPhotoStore> {
            val context = androidContext()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val keyMemory =
                object : AccountPhotoKeyMemory {
                    override var lastKey: String?
                        get() = prefs.getString(LAST_KEY, null)
                        set(value) {
                            prefs.edit().putString(LAST_KEY, value).apply()
                        }
                }
            val cache = AccountPhotoCache(SingletonImageLoader.get(context), keyMemory)
            object : AccountPhotoStore {
                override suspend fun store(
                    photoUrl: String,
                    bytes: ByteArray,
                ) = cache.store(photoUrl, bytes)

                override fun keepOnly(photoUrl: String?) = cache.keepOnly(photoUrl)

                override fun clear() = cache.clear()
            }
        }
    }

private const val PREFS_NAME = "account_photo"
private const val LAST_KEY = "last_cache_key"
