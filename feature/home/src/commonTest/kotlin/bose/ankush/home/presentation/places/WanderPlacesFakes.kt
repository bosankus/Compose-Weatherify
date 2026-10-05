package bose.ankush.home.presentation.places

import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.finder.domain.usecase.DeleteLocationUseCase
import bose.ankush.finder.domain.usecase.GetSavedLocationsUseCase
import bose.ankush.finder.domain.usecase.SaveLocationParams
import bose.ankush.finder.domain.usecase.SaveLocationUseCase
import bose.ankush.finder.domain.usecase.SearchPlacesUseCase
import bose.ankush.home.data.HomeSavedPlacesEntryImpl
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.api.PremiumStatus
import bose.ankush.storage.api.PremiumStorage
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory location preferences shared by the places and home ViewModels in tests. */
internal class FakeLocationPreferences(
    initial: LocationPreferences = LocationPreferences(),
) : LocationPreferencesStorage {
    val preferences = MutableStateFlow(initial)
    var overrideWrites = 0
        private set

    override fun getLocationPreferencesFlow(): Flow<LocationPreferences> = preferences

    override suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
        preferences.value = preferences.value.copy(latitude = coordinates.first, longitude = coordinates.second)
    }

    override suspend fun saveLocationOverride(
        lat: Double,
        lon: Double,
        name: String,
    ) {
        overrideWrites++
        preferences.value =
            preferences.value.copy(
                isLocationOverridden = true,
                overrideLat = lat,
                overrideLon = lon,
                overrideLocationName = name,
            )
    }

    override suspend fun clearLocationOverride() {
        preferences.value =
            preferences.value.copy(
                isLocationOverridden = false,
                overrideLat = null,
                overrideLon = null,
                overrideLocationName = null,
            )
    }

    override suspend fun clearAll() {
        preferences.value = LocationPreferences()
    }
}

internal class FakeFinder(
    val saved: MutableList<Location> = mutableListOf(),
) {
    var loadCalls = 0
    var loadResult: (() -> Result<List<Location>>)? = null
    val saveCalls = mutableListOf<SaveLocationParams>()
    var saveResult: Result<Unit> = Result.success(Unit)
    val deleteCalls = mutableListOf<String>()
    var deleteResult: Result<Unit> = Result.success(Unit)
    val searchCalls = mutableListOf<String>()
    var searchResult: (String) -> Result<List<LocationSuggestion>> = { Result.success(emptyList()) }

    val getSaved =
        object : GetSavedLocationsUseCase {
            override suspend fun invoke(): Result<List<Location>> {
                loadCalls++
                return loadResult?.invoke() ?: Result.success(saved.toList())
            }
        }

    val save =
        object : SaveLocationUseCase {
            override suspend fun invoke(params: SaveLocationParams): Result<Unit> {
                saveCalls += params
                if (saveResult.isSuccess) {
                    saved +=
                        Location(id = "id-${saved.size + 1}", name = params.name, lat = params.lat, lon = params.lon)
                }
                return saveResult
            }
        }

    val delete =
        object : DeleteLocationUseCase {
            override suspend fun invoke(id: String): Result<Unit> {
                deleteCalls += id
                if (deleteResult.isSuccess) saved.removeAll { it.id == id }
                return deleteResult
            }
        }

    val search =
        object : SearchPlacesUseCase {
            override suspend fun invoke(query: String): Result<List<LocationSuggestion>> {
                searchCalls += query
                return searchResult(query)
            }
        }
}

internal class FakePremium(
    isPremium: Boolean,
) : PremiumStorage {
    val status = MutableStateFlow(PremiumStatus(isPremium = isPremium))

    override fun observePremiumStatus(): Flow<PremiumStatus> = status

    override suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    ) {
        status.value = PremiumStatus(isPremium, expiryMillis)
    }
}

internal object NoopAnalytics : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}

internal fun placesViewModel(
    finder: FakeFinder = FakeFinder(),
    preferences: FakeLocationPreferences = FakeLocationPreferences(),
    premium: FakePremium = FakePremium(isPremium = true),
    savedPlacesEntry: HomeSavedPlacesEntryImpl = HomeSavedPlacesEntryImpl(),
): WanderPlacesViewModel =
    WanderPlacesViewModel(
        getSavedLocations = finder.getSaved,
        saveLocation = finder.save,
        deleteLocation = finder.delete,
        searchPlaces = finder.search,
        premiumStorage = premium,
        locationPreferences = preferences,
        analyticsTracker = NoopAnalytics,
        savedPlacesEntry = savedPlacesEntry,
    )

internal fun suggestion(
    name: String,
    lat: Double,
    lon: Double,
    country: String = "France",
): LocationSuggestion =
    LocationSuggestion(
        name = name,
        city = name,
        state = "",
        country = country,
        latitude = lat,
        longitude = lon,
    )
