package bose.ankush.finder.presentation.savedlocations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
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
