package bose.ankush.home.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceCardTrailingTest {
    @Test
    fun activePlaceShowsTickInsteadOfRemove() {
        assertEquals(
            PlaceCardTrailing.Selected,
            placeCardTrailing(isActive = true, canDelete = true)
        )
    }

    @Test
    fun activeGpsCardShowsTick() {
        assertEquals(
            PlaceCardTrailing.Selected,
            placeCardTrailing(isActive = true, canDelete = false)
        )
    }

    @Test
    fun inactiveRemovablePlaceKeepsRemoveButton() {
        assertEquals(
            PlaceCardTrailing.Remove,
            placeCardTrailing(isActive = false, canDelete = true)
        )
    }

    @Test
    fun inactiveGpsCardHasNoTrailingControl() {
        assertEquals(PlaceCardTrailing.None, placeCardTrailing(isActive = false, canDelete = false))
    }
}
