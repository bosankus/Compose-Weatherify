package bose.ankush.weatherify.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation3.SwipeDismissableSceneStrategy
import bose.ankush.weatherify.wear.data.WeatherSyncStore
import bose.ankush.weatherify.wear.data.WeatherUiMapper
import bose.ankush.weatherify.wear.presentation.navigation.AlertDetailRoute
import bose.ankush.weatherify.wear.presentation.navigation.WeatherRoute
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAlreadySyncedForecast()
        setContent { WeatherifyWearApp() }
    }

    /**
     * [WeatherSyncListenerService][bose.ankush.weatherify.wear.data.WeatherSyncListenerService]
     * only fires for sync events that happen while it's registered — it won't replay a sync that
     * landed before this process started. DataClient still has the last item on disk, so pull it
     * once on launch to cover that case.
     */
    private fun loadAlreadySyncedForecast() {
        lifecycleScope.launch {
            runCatching { Wearable.getDataClient(this@MainActivity).dataItems.await() }
                .onSuccess { buffer ->
                    buffer.mapNotNull { WeatherSyncStore.parse(it) }
                        .forEach {
                            WeatherSyncStore.update(it)
                            Timber.d("Loaded previously synced forecast: %s", it.locationName)
                        }
                    buffer.release()
                }
                .onFailure { Timber.w(it, "Failed to load previously synced forecast") }
        }
    }
}

@Composable
private fun WeatherifyWearApp() {
    val synced by WeatherSyncStore.synced.collectAsState()

    MaterialTheme {
        // Single AppScaffold at the root: it owns the TimeText overlay and keeps it fixed
        // while individual ScreenScaffolds swap underneath — nesting one per screen makes
        // the clock re-enter on every navigation, which reads as jank.
        AppScaffold {
            val synced = synced
            if (synced == null) {
                WaitingForSyncScreen()
            } else {
                WeatherNavHost(
                    uiState = remember(synced) {
                        WeatherUiMapper.mapToUiState(
                            synced.forecast,
                            synced.locationName
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun WeatherNavHost(uiState: WeatherUiState) {
    val backStack = rememberNavBackStack(WeatherRoute as NavKey)

    val entryProvider = entryProvider<NavKey> {
        entry<WeatherRoute> {
            WeatherScreen(
                uiState = uiState,
                onAlertClick = { backStack.add(AlertDetailRoute) }
            )
        }

        entry<AlertDetailRoute> {
            uiState.alert?.let {
                AlertDetailScreen(alert = it)
            }
        }
    }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider
    )

    NavDisplay(
        entries = entries,
        sceneStrategies = listOf(SwipeDismissableSceneStrategy()),
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
    )
}
