package bose.ankush.storage.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.KeystoreDpopKey
import bose.ankush.storage.LocationPreferencesStorageImpl
import bose.ankush.storage.PremiumStorageImpl
import bose.ankush.storage.WeatherStorageImpl
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.api.PremiumStorage
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.LOCATION_PREFERENCES_FILE_NAME
import bose.ankush.storage.common.PREMIUM_PREFERENCES_FILE_NAME
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import bose.ankush.storage.room.MIGRATION_3_4
import bose.ankush.storage.room.MIGRATION_4_5
import bose.ankush.storage.room.MIGRATION_5_6
import bose.ankush.storage.room.WeatherDao
import bose.ankush.storage.room.WeatherDataModelConverters
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.setApplicationContext
import bose.ankush.tokenstorage.api.DpopKey
import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val premiumPreferencesQualifier = named("premiumPreferencesDataStore")

actual val storageDomainModule: Module =
    module {
        single<WeatherDataModelConverters> { WeatherDataModelConverters() }
        single<WeatherDao> {
            val context = androidContext()
            val converters = get<WeatherDataModelConverters>()

            val dbDeferred: CompletableDeferred<WeatherDatabase> = CompletableDeferred()

            // Start database build IMMEDIATELY on IO dispatcher during Koin module loading
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val db = androidx.room.Room
                        .databaseBuilder(
                            context.applicationContext,
                            WeatherDatabase::class.java,
                            WEATHER_DATABASE_NAME,
                        )
                        .addTypeConverter(converters)
                        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                        .fallbackToDestructiveMigration(false)
                        .build()
                    dbDeferred.complete(db)
                } catch (e: Throwable) {
                    dbDeferred.completeExceptionally(e)
                }
            }

            // Return DAO, waiting for build if not yet complete
            runBlocking { dbDeferred.await().weatherDao() }
        }
        
        single<WeatherStorage> { WeatherStorageImpl(get()) }
        single<DpopKey> { KeystoreDpopKey() }
        single<TokenStorage> {
            setApplicationContext(androidContext())
            EncryptedTokenStorageImpl()
        }
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    androidContext()
                        .filesDir
                        .resolve(LOCATION_PREFERENCES_FILE_NAME)
                        .absolutePath
                        .toPath()
                },
            )
        }
        single<LocationPreferencesStorage> { LocationPreferencesStorageImpl(get()) }
        single<DataStore<Preferences>>(premiumPreferencesQualifier) {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    androidContext()
                        .filesDir
                        .resolve("datastore")
                        .resolve(PREMIUM_PREFERENCES_FILE_NAME)
                        .absolutePath
                        .toPath()
                },
            )
        }
        single<PremiumStorage> { PremiumStorageImpl(get(premiumPreferencesQualifier)) }
    }
