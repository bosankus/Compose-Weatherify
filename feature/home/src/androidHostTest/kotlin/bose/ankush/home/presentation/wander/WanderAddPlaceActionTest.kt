package bose.ankush.home.presentation.wander

import bose.ankush.finder.domain.model.LocationSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WanderAddPlaceActionTest {
    private val paris = suggestion("Paris", 48.8566, 2.3522)
    private val pune = suggestion("Pune", 18.5204, 73.8567)

    @Test
    fun nothingPickedDisablesTheButton() {
        val action = WanderAddPlaceAction.of(selected = null, isSaving = false, isSaved = { false })
        assertEquals(WanderAddPlaceAction.PickFirst, action)
        assertFalse(action.enabled)
    }

    @Test
    fun newPickAddsThePlace() {
        val action = WanderAddPlaceAction.of(selected = paris, isSaving = false, isSaved = { false })
        assertEquals(WanderAddPlaceAction.Add, action)
        assertTrue(action.enabled)
    }

    @Test
    fun savedPickOnlyShowsIt() {
        val action = WanderAddPlaceAction.of(selected = paris, isSaving = false, isSaved = { it == paris })
        assertEquals(WanderAddPlaceAction.ShowSaved, action)
        assertTrue(action.enabled)
    }

    @Test
    fun savingWinsAndDisablesTheButton() {
        val action = WanderAddPlaceAction.of(selected = paris, isSaving = true, isSaved = { false })
        assertEquals(WanderAddPlaceAction.Saving, action)
        assertFalse(action.enabled)
    }

    @Test
    fun pickSurvivesWhileStillInResults() {
        assertEquals(paris, WanderAddPlaceAction.retainSelection(paris, listOf(pune, paris)))
    }

    @Test
    fun newResultsWithoutThePickClearIt() {
        assertNull(WanderAddPlaceAction.retainSelection(paris, listOf(pune)))
        assertNull(WanderAddPlaceAction.retainSelection(paris, emptyList()))
        assertNull(WanderAddPlaceAction.retainSelection(null, listOf(pune)))
    }

    @Test
    fun addButtonHidesWhileSheetIsOpen() {
        assertTrue(WanderAddPlaceAction.showAddButton(onPlacesPage = true, isPremium = true, sheetOpen = false))
        assertFalse(WanderAddPlaceAction.showAddButton(onPlacesPage = true, isPremium = true, sheetOpen = true))
    }

    @Test
    fun addButtonNeedsPlacesPageAndPremium() {
        assertFalse(WanderAddPlaceAction.showAddButton(onPlacesPage = false, isPremium = true, sheetOpen = false))
        assertFalse(WanderAddPlaceAction.showAddButton(onPlacesPage = true, isPremium = false, sheetOpen = false))
    }

    private fun suggestion(
        name: String,
        lat: Double,
        lon: Double,
    ) = LocationSuggestion(name = name, city = name, state = "", country = "", latitude = lat, longitude = lon)
}
