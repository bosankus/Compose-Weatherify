package bose.ankush.home.di

import androidx.work.WorkManager
import bose.ankush.home.PlaceEventSyncTrigger
import bose.ankush.home.data.ai.createOnDeviceAiClient
import bose.ankush.home.data.events.sync.PlaceEventSyncScheduler
import bose.ankush.home.data.events.sync.WorkManagerPlaceEventSyncScheduler
import bose.ankush.home.data.events.sync.WorkManagerPlaceEventSyncWorkQueue
import bose.ankush.home.data.location.AndroidHomeGeocoder
import bose.ankush.home.data.location.DeviceLocationClient
import bose.ankush.home.data.remoteconfig.FirebaseHomeRemoteConfigGate
import bose.ankush.home.data.wear.AndroidWeatherWearSync
import bose.ankush.home.domain.ai.OnDeviceAiClient
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.tokenstorage.api.TokenStorage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

actual val homePlatformModule: Module =
    module {
        single<LocationClient> {
            DeviceLocationClient(
                context = androidContext(),
                client = LocationServices.getFusedLocationProviderClient(androidContext()),
            )
        }
        single<HomeRemoteConfigGate> { FirebaseHomeRemoteConfigGate() }
        single<HomeGeocoder> { AndroidHomeGeocoder(androidContext()) }
        single<WeatherWearSync> { AndroidWeatherWearSync(androidContext()) }
        single<OnDeviceAiClient> { createOnDeviceAiClient() }
        // Place-event sync on WorkManager (default initializer; the worker reads Koin when it runs).
        // Started and cancelled with the session (a stored JWT), like the rest of the app.
        single {
            val context = androidContext()
            WorkManagerPlaceEventSyncScheduler(
                queue = WorkManagerPlaceEventSyncWorkQueue { WorkManager.getInstance(context) },
                engine = get(),
                isSignedIn = placeEventSyncSignedIn(),
                signedInChanges = get<TokenStorage>().hasToken(),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            )
        } binds arrayOf(PlaceEventSyncScheduler::class, PlaceEventSyncTrigger::class)
    }
