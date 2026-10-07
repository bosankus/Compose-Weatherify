package bose.ankush.home.presentation.screen

import bose.ankush.finder.domain.model.LocationSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddPlaceActionTest {
    private val paris = suggestion("Paris", 48.8566, 2.3522)
    private val pune = suggestion("Pune", 18.5204, 73.8567)

    @Test
    fun nothingPickedDisablesTheButton() {
        val action = AddPlaceAction.of(selected = null, isSaving = false, isSaved = { false })
        assertEquals(AddPlaceAction.PickFirst, action)
        assertFalse(action.enabled)
    }

    @Test
    fun newPickAddsThePlace() {
        val action = AddPlaceAction.of(selected = paris, isSaving = false, isSaved = { false })
        assertEquals(AddPlaceAction.Add, action)
        assertTrue(action.enabled)
    }

    @Test
    fun savedPickOnlyShowsIt() {
        val action =
            AddPlaceAction.of(selected = paris, isSaving = false, isSaved = { it == paris })
        assertEquals(AddPlaceAction.ShowSaved, action)
        assertTrue(action.enabled)
    }

    @Test
    fun savingWinsAndDisablesTheButton() {
        val action = AddPlaceAction.of(selected = paris, isSaving = true, isSaved = { false })
        assertEquals(AddPlaceAction.Saving, action)
        assertFalse(action.enabled)
    }

    @Test
    fun pickSurvivesWhileStillInResults() {
        assertEquals(paris, AddPlaceAction.retainSelection(paris, listOf(pune, paris)))
    }

    @Test
    fun newResultsWithoutThePickClearIt() {
        assertNull(AddPlaceAction.retainSelection(paris, listOf(pune)))
        assertNull(AddPlaceAction.retainSelection(paris, emptyList()))
        assertNull(AddPlaceAction.retainSelection(null, listOf(pune)))
    }

    @Test
    fun addButtonHidesWhileSheetIsOpen() {
        assertTrue(
            AddPlaceAction.showAddButton(
                onPlacesPage = true,
                isPremium = true,
                sheetOpen = false
            )
        )
        assertFalse(
            AddPlaceAction.showAddButton(
                onPlacesPage = true,
                isPremium = true,
                sheetOpen = true
            )
        )
    }

    @Test
    fun addButtonNeedsPlacesPageAndPremium() {
        assertFalse(
            AddPlaceAction.showAddButton(
                onPlacesPage = false,
                isPremium = true,
                sheetOpen = false
            )
        )
        assertFalse(
            AddPlaceAction.showAddButton(
                onPlacesPage = true,
                isPremium = false,
                sheetOpen = false
            )
        )
    }

    private fun suggestion(
        name: String,
        lat: Double,
        lon: Double,
    ) = LocationSuggestion(name = name, city = name, state = "", country = "", latitude = lat, longitude = lon)
}
