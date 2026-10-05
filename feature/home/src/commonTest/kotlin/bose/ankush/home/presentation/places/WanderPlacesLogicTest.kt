package bose.ankush.home.presentation.places

import bose.ankush.finder.domain.model.Location
import bose.ankush.storage.model.LocationPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WanderPlacesLogicTest {
    private val paris = Location(id = "paris", name = "Paris", lat = 48.8566, lon = 2.3522)

    @Test
    fun queriesNeedTwoNonBlankCharacters() {
        assertFalse(WanderPlacesLogic.isSearchable(""))
        assertFalse(WanderPlacesLogic.isSearchable(" P "))
        assertTrue(WanderPlacesLogic.isSearchable(" Pa"))
    }

    @Test
    fun activePlaceIsTheOverrideOnly() {
        assertNull(WanderPlacesLogic.activePlace(LocationPreferences(latitude = 1.0, longitude = 2.0)))
        assertNull(
            WanderPlacesLogic.activePlace(LocationPreferences(isLocationOverridden = true, overrideLat = 1.0)),
        )
        assertEquals(
            ActivePlace(lat = 1.0, lon = 2.0, name = "Home"),
            WanderPlacesLogic.activePlace(
                LocationPreferences(
                    isLocationOverridden = true,
                    overrideLat = 1.0,
                    overrideLon = 2.0,
                    overrideLocationName = "Home",
                ),
            ),
        )
    }

    @Test
    fun activeMatchToleratesRoundingButNotOtherCities() {
        assertTrue(WanderPlacesLogic.isActive(paris, ActivePlace(48.8567, 2.3521, null)))
        assertFalse(WanderPlacesLogic.isActive(paris, ActivePlace(48.87, 2.3522, null)))
        assertFalse(WanderPlacesLogic.isActive(paris, null))
    }

    @Test
    fun findsAnAlreadySavedSuggestion() {
        val places = listOf(paris)
        assertEquals(paris, WanderPlacesLogic.findSaved(suggestion("Paris", 48.8566, 2.3522), places))
        assertNull(WanderPlacesLogic.findSaved(suggestion("Lyon", 45.764, 4.8357), places))
    }

    @Test
    fun savedNameFallsBackToCityThenCoordinates() {
        assertEquals("Paris", WanderPlacesLogic.savedName(suggestion(" Paris ", 1.0, 2.0)))
        assertEquals(
            "Lyon",
            WanderPlacesLogic.savedName(suggestion("Lyon", 1.0, 2.0).copy(name = " ")),
        )
        assertEquals(
            "1.0, 2.0",
            WanderPlacesLogic.savedName(suggestion("", 1.0, 2.0).copy(city = "")),
        )
    }

    @Test
    fun detailSkipsBlanksAndTheName() {
        val result = suggestion("Paris", 1.0, 2.0).copy(state = "Île-de-France")
        assertEquals("Île-de-France, France", WanderPlacesLogic.suggestionDetail(result))
    }

    @Test
    fun distinctPlacesKeepsOneCardPerPlace() {
        val noId = Location(id = "", name = "Lyon", lat = 45.0, lon = 4.0)
        val places = listOf(paris, paris.copy(name = "Paris again"), noId, noId)
        assertEquals(listOf(paris, noId), WanderPlacesLogic.distinctPlaces(places))
    }

    @Test
    fun coordinatesAreRoundedToFourPlaces() {
        assertEquals("48.8566, 2.3522", WanderPlacesLogic.formatCoordinates(48.856614, 2.3522219))
    }
}
