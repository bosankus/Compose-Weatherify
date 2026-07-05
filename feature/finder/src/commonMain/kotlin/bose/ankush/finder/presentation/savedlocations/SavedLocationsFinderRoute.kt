package bose.ankush.finder.presentation.savedlocations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * Public entry point for the saved-locations / place-search feature.
 *
 * [SavedLocationsViewModel], [PlaceSearchViewModel], and all MVI Intent/State/Effect
 * types are `internal` to this module — callers only see this composable. All UI
 * strings are owned by this module's Compose Multiplatform resources. The ViewModel
 * is resolved here via Koin, so its lifetime is scoped to wherever this composable
 * is placed in composition (e.g. a single navigation entry), not shared globally
 * with the host app.
 */
@Composable
fun SavedLocationsFinderRoute(
    onLocationSelected: (lat: Double, lon: Double, name: String) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val viewModel = koinViewModel<SavedLocationsViewModel>()
    val state by viewModel.state.collectAsState()

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
