package bose.ankush.home.domain.nearby

import bose.ankush.network.model.Account
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.model.DeleteAccountResult
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SavedLocation
import bose.ankush.network.repository.AccountRepository
import bose.ankush.network.repository.LocationRepository
import bose.ankush.network.repository.PlaceEventRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Place events keyed by request. [gate] lets a test hold one location's response open
 * to check that a newer location wins.
 */
internal class FakePlaceEventRepository(
    var events: (lat: Double, lon: Double) -> Result<List<PlaceEvent>> = { _, _ ->
        Result.success(
            emptyList(),
        )
    },
    var createResult: Result<Unit> = Result.success(Unit),
) : PlaceEventRepository {
    val gates = mutableMapOf<Pair<Double, Double>, CompletableDeferred<Unit>>()
    val requested = mutableListOf<Pair<Double, Double>>()
    val created = mutableListOf<CreatePlaceEventRequest>()

    override suspend fun getPlaceEvents(
        lat: Double,
        lon: Double,
    ): Result<List<PlaceEvent>> {
        requested += lat to lon
        gates[lat to lon]?.await()
        return events(lat, lon)
    }

    override suspend fun createPlaceEvent(request: CreatePlaceEventRequest): Result<Unit> {
        created += request
        return createResult
    }
}

internal class FakeLocationRepository(
    var saved: Result<List<SavedLocation>> = Result.success(emptyList()),
) : LocationRepository {
    var savedCalls = 0
        private set

    override suspend fun getSavedLocations(): Result<List<SavedLocation>> {
        savedCalls++
        return saved
    }

    override suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteLocation(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun searchPlaces(query: String): Result<List<PlaceSuggestion>> = Result.success(emptyList())
}

internal class FakeAccountRepository(
    var fetched: Result<Account> = Result.success(Account()),
) : AccountRepository {
    private val _account = MutableStateFlow<Account?>(null)
    override val account: StateFlow<Account?> = _account

    override suspend fun getAccount(): Result<Account> = fetched.onSuccess { _account.value = it }

    override suspend fun uploadPhoto(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
    ): Result<String?> = Result.success(null)

    override suspend fun deletePhoto(): Result<Unit> = Result.success(Unit)

    override suspend fun deleteAccount(password: String): DeleteAccountResult = DeleteAccountResult.Deleted

    override fun clear() {
        _account.value = null
    }

    /** What another screen (settings) writes to the shared account. */
    fun publish(account: Account?) {
        _account.value = account
    }
}
