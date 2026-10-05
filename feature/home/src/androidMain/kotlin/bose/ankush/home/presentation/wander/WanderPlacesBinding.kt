package bose.ankush.home.presentation.wander

import bose.ankush.home.presentation.places.WanderPlacesEffect
import bose.ankush.home.presentation.places.WanderPlacesIntent
import bose.ankush.home.presentation.places.WanderPlacesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Places page wiring: state and effects from the places ViewModel, intents back to it. */
internal data class WanderPlacesBinding(
    val state: WanderPlacesState = WanderPlacesState(),
    val effects: Flow<WanderPlacesEffect> = emptyFlow(),
    val onIntent: (WanderPlacesIntent) -> Unit = {},
    val onUpgrade: () -> Unit = {},
)
