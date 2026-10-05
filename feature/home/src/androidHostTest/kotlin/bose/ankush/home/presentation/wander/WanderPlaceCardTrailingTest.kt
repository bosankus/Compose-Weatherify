package bose.ankush.home.presentation.wander

import kotlin.test.Test
import kotlin.test.assertEquals

class WanderPlaceCardTrailingTest {
    @Test
    fun activePlaceShowsTickInsteadOfRemove() {
        assertEquals(WanderPlaceCardTrailing.Selected, placeCardTrailing(isActive = true, canDelete = true))
    }

    @Test
    fun activeGpsCardShowsTick() {
        assertEquals(WanderPlaceCardTrailing.Selected, placeCardTrailing(isActive = true, canDelete = false))
    }

    @Test
    fun inactiveRemovablePlaceKeepsRemoveButton() {
        assertEquals(WanderPlaceCardTrailing.Remove, placeCardTrailing(isActive = false, canDelete = true))
    }

    @Test
    fun inactiveGpsCardHasNoTrailingControl() {
        assertEquals(WanderPlaceCardTrailing.None, placeCardTrailing(isActive = false, canDelete = false))
    }
}
