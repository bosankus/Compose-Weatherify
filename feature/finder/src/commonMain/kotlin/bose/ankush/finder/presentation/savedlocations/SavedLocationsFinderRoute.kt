package bose.ankush.finder.presentation.savedlocations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Public entry point for the saved-locations / place-search feature.
 */
@Composable
fun SavedLocationsFinderRoute(
    onLocationSelected: (lat: Double, lon: Double, name: String) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val viewModel = koinViewModel<SavedLocationsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel.effect) {
        viewModel.effect
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
            if (effect is SavedLocationsEffect.LocationSelected) {
                onLocationSelected(effect.lat, effect.lon, effect.name)
            }
        }
    }

    SavedLocationsScreen(
        state = state,
        onIntent = viewModel::processIntent,
        bottomBar = bottomBar,
    )
}
