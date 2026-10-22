package bose.ankush.home.data.events

import bose.ankush.home.PlaceEventSyncTrigger
import bose.ankush.home.data.events.sync.PlaceEventSyncScheduler
import bose.ankush.home.di.homeDomainModule
import bose.ankush.home.domain.repository.PlaceEventsRepository
import bose.ankush.home.domain.usecase.CreateNearbyEvent
import bose.ankush.home.domain.usecase.DeletePlaceEvent
import bose.ankush.home.domain.usecase.GetNearbyEvents
import bose.ankush.home.domain.usecase.ObservePlaceEvents
import bose.ankush.home.domain.usecase.UpdatePlaceEvent
import bose.ankush.network.api.PlaceEventSyncApiService
import bose.ankush.storage.api.PlaceEventStorage
import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertSame

class PlaceEventsKoinWiringTest {
    @Test
    fun homeDomainModuleWiresTheOfflineFirstGraph() {
        val koin =
            koinApplication {
                modules(
                    module {
                        single<PlaceEventStorage> { FakePlaceEventStorage() }
                        single<PlaceEventSyncApiService> { FakePlaceEventSyncApi() }
                        single<TokenStorage> { SignedOutTokenStorage }
                    },
                    homeDomainModule,
                )
            }.koin

        val scheduler = koin.get<PlaceEventSyncScheduler>()
        assertSame(scheduler, koin.get<PlaceEventSyncTrigger>())
        koin.get<PlaceEventsRepository>()
        koin.get<NearbyPlaceEventSource>()
        koin.get<GetNearbyEvents>()
        koin.get<CreateNearbyEvent>()
        koin.get<ObservePlaceEvents>()
        koin.get<UpdatePlaceEvent>()
        koin.get<DeletePlaceEvent>()
        koin.close()
    }
}

private object SignedOutTokenStorage : TokenStorage {
    override suspend fun saveToken(token: String) = Unit

    override suspend fun getToken(): String? = null

    override fun hasToken(): Flow<Boolean> = flowOf(false)

    override suspend fun clearToken() = Unit
}
