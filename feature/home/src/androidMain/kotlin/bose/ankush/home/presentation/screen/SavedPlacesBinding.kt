package bose.ankush.home.presentation.screen

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
)
