package bose.ankush.home.presentation.places

import bose.ankush.finder.domain.model.Location
import bose.ankush.home.data.HomeSavedPlacesEntryImpl
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedPlacesViewModelTest {
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun TestScope.useTestMain() = Dispatchers.setMain(StandardTestDispatcher(testScheduler))

    private fun TestScope.collectEffects(viewModel: SavedPlacesViewModel): List<SavedPlacesEffect> {
        val effects = mutableListOf<SavedPlacesEffect>()
        // Unconfined: advanceUntilIdle does not wait for background work on a standard dispatcher.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    @Test
    fun premiumLoadsSavedPlaces() =
        runTest {
            useTestMain()
            val finder = FakeFinder(mutableListOf(PARIS, TOKYO))
            val viewModel = placesViewModel(finder = finder)
            advanceUntilIdle()

            assertEquals(listOf(PARIS, TOKYO), viewModel.state.value.places)
            assertTrue(viewModel.state.value.isPremium)
            assertFalse(viewModel.state.value.isLoading)
            assertEquals(1, finder.loadCalls)
        }

    @Test
    fun freeAccountNeverLoadsSavedPlaces() =
        runTest {
            useTestMain()
            val finder = FakeFinder(mutableListOf(PARIS))
            val viewModel = placesViewModel(finder = finder, premium = FakePremium(isPremium = false))
            advanceUntilIdle()
            viewModel.processIntent(SavedPlacesIntent.Load)
            advanceUntilIdle()

            assertEquals(0, finder.loadCalls)
            assertTrue(
                viewModel.state.value.places
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.isPremium)
        }

    @Test
    fun loadFailureIsShownAndRetryRecovers() =
        runTest {
            useTestMain()
            val finder = FakeFinder(mutableListOf(PARIS))
            finder.loadResult = { Result.failure(IllegalStateException("offline")) }
            val viewModel = placesViewModel(finder = finder)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.loadFailed)

            finder.loadResult = null
            viewModel.processIntent(SavedPlacesIntent.Load)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.loadFailed)
            assertEquals(listOf(PARIS), viewModel.state.value.places)
        }

    @Test
    fun selectingSavedPlaceMakesItActiveAndShowsWeather() =
        runTest {
            useTestMain()
            val preferences = FakeLocationPreferences(LocationPreferences(latitude = GPS_LAT, longitude = GPS_LON))
            val viewModel = placesViewModel(finder = FakeFinder(mutableListOf(PARIS, TOKYO)), preferences = preferences)
            val effects = collectEffects(viewModel)
            advanceUntilIdle()
            assertNull(viewModel.state.value.active)

            viewModel.processIntent(SavedPlacesIntent.Select(TOKYO))
            advanceUntilIdle()

            val prefs = preferences.preferences.value
            assertTrue(prefs.isLocationOverridden)
            assertEquals(TOKYO.lat, prefs.overrideLat)
            assertEquals(TOKYO.lon, prefs.overrideLon)
            assertEquals(TOKYO.name, prefs.overrideLocationName)
            assertEquals(ActivePlace(TOKYO.lat, TOKYO.lon, TOKYO.name), viewModel.state.value.active)
            assertEquals(listOf<SavedPlacesEffect>(SavedPlacesEffect.ShowWeather), effects)
        }

    @Test
    fun selectingTheActivePlaceOnlyShowsWeather() =
        runTest {
            useTestMain()
            val preferences = FakeLocationPreferences()
            val viewModel = placesViewModel(finder = FakeFinder(mutableListOf(PARIS)), preferences = preferences)
            val effects = collectEffects(viewModel)
            viewModel.processIntent(SavedPlacesIntent.Select(PARIS))
            advanceUntilIdle()
            viewModel.processIntent(SavedPlacesIntent.Select(PARIS))
            advanceUntilIdle()

            assertEquals(1, preferences.overrideWrites)
            assertEquals(2, effects.size)
        }

    @Test
    fun currentLocationClearsTheOverride() =
        runTest {
            useTestMain()
            val preferences = FakeLocationPreferences()
            val viewModel = placesViewModel(finder = FakeFinder(mutableListOf(PARIS)), preferences = preferences)
            val effects = collectEffects(viewModel)
            viewModel.processIntent(SavedPlacesIntent.Select(PARIS))
            advanceUntilIdle()

            viewModel.processIntent(SavedPlacesIntent.UseCurrentLocation)
            advanceUntilIdle()

            assertFalse(preferences.preferences.value.isLocationOverridden)
            assertNull(viewModel.state.value.active)
            assertEquals(2, effects.size)
        }

    @Test
    fun searchIsDebouncedToTheLastQuery() =
        runTest {
            useTestMain()
            val finder = FakeFinder()
            finder.searchResult = { Result.success(listOf(suggestion("Paris", PARIS.lat, PARIS.lon))) }
            val viewModel = placesViewModel(finder = finder)
            advanceUntilIdle()

            viewModel.processIntent(SavedPlacesIntent.OpenSearch)
            viewModel.processIntent(SavedPlacesIntent.QueryChanged("Pa"))
            advanceTimeBy(DEBOUNCE_MS / 2)
            viewModel.processIntent(SavedPlacesIntent.QueryChanged("Par"))
            advanceTimeBy(DEBOUNCE_MS / 2)
            viewModel.processIntent(SavedPlacesIntent.QueryChanged("Pari"))
            runCurrent()
            assertTrue(viewModel.state.value.search.isSearching)
            assertTrue(finder.searchCalls.isEmpty())

            advanceTimeBy(DEBOUNCE_MS + 1)
            runCurrent()

            assertEquals(listOf("Pari"), finder.searchCalls)
            val search = viewModel.state.value.search
            assertTrue(search.isOpen)
            assertEquals("Pari", search.query)
            assertEquals(listOf("Paris"), search.results.map { it.name })
            assertFalse(search.isSearching)
        }

    @Test
    fun shortQueryNeverSearchesAndClearsResults() =
        runTest {
            useTestMain()
            val finder = FakeFinder()
            finder.searchResult = { Result.success(listOf(suggestion("Paris", PARIS.lat, PARIS.lon))) }
            val viewModel = placesViewModel(finder = finder)
            viewModel.processIntent(SavedPlacesIntent.OpenSearch)
            viewModel.processIntent(SavedPlacesIntent.QueryChanged("Paris"))
            advanceUntilIdle()
            assertEquals(1, viewModel.state.value.search.results.size)

            viewModel.processIntent(SavedPlacesIntent.QueryChanged("P"))
            assertTrue(
                viewModel.state.value.search.results
                    .isEmpty(),
            )
            advanceUntilIdle()

            assertEquals(listOf("Paris"), finder.searchCalls)
            assertFalse(viewModel.state.value.search.isSearching)
        }

    @Test
    fun searchFailureIsFlagged() =
        runTest {
            useTestMain()
            val finder = FakeFinder()
            finder.searchResult = { Result.failure(IllegalStateException("offline")) }
            val viewModel = placesViewModel(finder = finder)
            viewModel.processIntent(SavedPlacesIntent.OpenSearch)
            viewModel.processIntent(SavedPlacesIntent.QueryChanged("Paris"))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.search.searchFailed)
            assertFalse(viewModel.state.value.search.isSearching)
        }

    @Test
    fun savingASuggestionStoresItMakesItActiveAndClosesTheSheet() =
        runTest {
            useTestMain()
            val finder = FakeFinder(mutableListOf(TOKYO))
            val preferences = FakeLocationPreferences()
            val viewModel = placesViewModel(finder = finder, preferences = preferences)
            val effects = collectEffects(viewModel)
            advanceUntilIdle()
            viewModel.processIntent(SavedPlacesIntent.OpenSearch)

            viewModel.processIntent(
                SavedPlacesIntent.SaveSuggestion(
                    suggestion(
                        "Paris",
                        PARIS.lat,
                        PARIS.lon
                    )
                )
            )
            advanceUntilIdle()

            assertEquals(1, finder.saveCalls.size)
            assertEquals("Paris", finder.saveCalls.single().name)
            assertEquals(PARIS.lat, finder.saveCalls.single().lat)
            val prefs = preferences.preferences.value
            assertTrue(prefs.isLocationOverridden)
            assertEquals(PARIS.lat, prefs.overrideLat)
            assertEquals("Paris", prefs.overrideLocationName)
            val state = viewModel.state.value
            assertFalse(state.search.isOpen)
            assertEquals("", state.search.query)
            assertEquals(listOf("Tokyo", "Paris"), state.places.map { it.name })
            assertTrue(state.places.all { it.id.isNotBlank() })
            assertEquals(2, finder.loadCalls)
            assertEquals(listOf<SavedPlacesEffect>(SavedPlacesEffect.ShowWeather), effects)
        }

    @Test
    fun saveFailureKeepsTheSheetOpenAndTheActiveLocation() =
        runTest {
            useTestMain()
            val finder = FakeFinder()
            finder.saveResult = Result.failure(IllegalStateException("offline"))
            val preferences = FakeLocationPreferences()
            val viewModel = placesViewModel(finder = finder, preferences = preferences)
            val effects = collectEffects(viewModel)
            viewModel.processIntent(SavedPlacesIntent.OpenSearch)

            viewModel.processIntent(
                SavedPlacesIntent.SaveSuggestion(
                    suggestion(
                        "Paris",
                        PARIS.lat,
                        PARIS.lon
                    )
                )
            )
            advanceUntilIdle()

            val search = viewModel.state.value.search
            assertTrue(search.isOpen)
            assertTrue(search.saveFailed)
            assertFalse(search.isSaving)
            assertFalse(preferences.preferences.value.isLocationOverridden)
            assertTrue(effects.isEmpty())
        }

    @Test
    fun savingAnAlreadySavedPlaceOnlyMakesItActive() =
        runTest {
            useTestMain()
            val finder = FakeFinder(mutableListOf(PARIS))
            val preferences = FakeLocationPreferences()
            val viewModel = placesViewModel(finder = finder, preferences = preferences)
            advanceUntilIdle()
            viewModel.processIntent(SavedPlacesIntent.OpenSearch)

            viewModel.processIntent(
                SavedPlacesIntent.SaveSuggestion(
                    suggestion(
                        "Paris",
                        PARIS.lat + 0.0001,
                        PARIS.lon
                    )
                ),
            )
            advanceUntilIdle()

            assertTrue(finder.saveCalls.isEmpty())
            assertEquals(PARIS.lat, preferences.preferences.value.overrideLat)
            assertFalse(viewModel.state.value.search.isOpen)
        }

    @Test
    fun deleteRemovesThePlaceAndFailureShowsANotice() =
        runTest {
            useTestMain()
            val finder = FakeFinder(mutableListOf(PARIS, TOKYO))
            val viewModel = placesViewModel(finder = finder)
            advanceUntilIdle()

            viewModel.processIntent(SavedPlacesIntent.Delete(PARIS.id))
            advanceUntilIdle()
            assertEquals(listOf(TOKYO), viewModel.state.value.places)

            finder.deleteResult = Result.failure(IllegalStateException("offline"))
            viewModel.processIntent(SavedPlacesIntent.Delete(TOKYO.id))
            advanceUntilIdle()
            assertEquals(listOf(TOKYO), viewModel.state.value.places)
            assertEquals(SavedPlacesNotice.DeleteFailed, viewModel.state.value.notice)

            viewModel.processIntent(SavedPlacesIntent.Load)
            assertNull(viewModel.state.value.notice)
        }

    @Test
    fun savedLocationsTabRequestOpensThePlacesPage() =
        runTest {
            useTestMain()
            val entry = HomeSavedPlacesEntryImpl()
            val finder = FakeFinder(mutableListOf(PARIS))
            val viewModel = placesViewModel(finder = finder, savedPlacesEntry = entry)
            val effects = collectEffects(viewModel)
            advanceUntilIdle()

            assertTrue(entry.openSavedPlaces())
            advanceUntilIdle()

            assertEquals(listOf<SavedPlacesEffect>(SavedPlacesEffect.ShowPlaces), effects)
            assertFalse(entry.pending.value)
            assertEquals(2, finder.loadCalls)
        }

    private companion object {
        const val GPS_LAT = 22.57
        const val GPS_LON = 88.36
        const val DEBOUNCE_MS = 400L
        val PARIS = Location(id = "paris", name = "Paris", lat = 48.8566, lon = 2.3522)
        val TOKYO = Location(id = "tokyo", name = "Tokyo", lat = 35.6762, lon = 139.6503)
    }
}
