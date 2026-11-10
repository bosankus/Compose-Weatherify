package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.presentation.places.HomeProgress
import bose.ankush.home.presentation.places.SavedPlacesEffect
import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.SavedPlacesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Places page wiring: state and effects from the places ViewModel, intents back to it. */
internal data class SavedPlacesBinding(
    val state: SavedPlacesState = SavedPlacesState(),
    val effects: Flow<SavedPlacesEffect> = emptyFlow(),
    val onIntent: (SavedPlacesIntent) -> Unit = {},
    val onUpgrade: () -> Unit = {},
    /** Where the map behind the page centres: the place Home is showing. Null until it is known. */
    val mapCenter: GeoPoint? = null,
    /** Home's forecast progress; the selected card spins until it lands. */
    val home: HomeProgress = HomeProgress(),
)
