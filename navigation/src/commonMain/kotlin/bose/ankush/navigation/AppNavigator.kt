package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val TAB_ROUTES: List<NavKey> = listOf(HomeRoute, SavedLocationsRoute, SettingsRoute)

// rememberNavBackStack's reflection-based, SavedStateConfiguration-free overload is Android-only;
// commonMain code must supply a SerializersModule that registers every NavKey subtype explicitly.
private val navKeySavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(HomeRoute::class)
                    subclass(SavedLocationsRoute::class)
                    subclass(SettingsRoute::class)
                    subclass(LanguageRoute::class)
                }
            }
    }

private val TabRouteSaver =
    Saver<MutableState<NavKey>, Int>(
        save = { state -> TAB_ROUTES.indexOf(state.value).coerceAtLeast(0) },
        restore = { index -> mutableStateOf(TAB_ROUTES.getOrElse(index) { HomeRoute }) },
    )

class AppNavigationState(
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    val startRoute: NavKey = HomeRoute
    var topLevelRoute: NavKey by topLevelRoute

    val currentStack: NavBackStack<NavKey>
        get() = backStacks[topLevelRoute] ?: error("No back stack for $topLevelRoute")

    fun isTabRoute(route: NavKey): Boolean = route in backStacks.keys
}

@Composable
fun rememberAppNavigationState(): AppNavigationState {
    val topLevelRoute =
        rememberSaveable(saver = TabRouteSaver) {
            mutableStateOf(HomeRoute)
        }
    val homeStack = rememberNavBackStack(navKeySavedStateConfiguration, HomeRoute)
    val savedLocationsStack = rememberNavBackStack(navKeySavedStateConfiguration, SavedLocationsRoute)
    val settingsStack = rememberNavBackStack(navKeySavedStateConfiguration, SettingsRoute)

    return remember {
        AppNavigationState(
            topLevelRoute = topLevelRoute,
            backStacks =
                mapOf(
                    HomeRoute to homeStack,
                    SavedLocationsRoute to savedLocationsStack,
                    SettingsRoute to settingsStack,
                ),
        )
    }
}

@Composable
fun AppNavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
    val saveableDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val vmDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()

    val homeEntries =
        rememberDecoratedNavEntries(
            backStack = backStacks[HomeRoute]!!,
            entryDecorators = listOf(saveableDecorator, vmDecorator),
            entryProvider = entryProvider,
        )
    val savedLocationsEntries =
        rememberDecoratedNavEntries(
            backStack = backStacks[SavedLocationsRoute]!!,
            entryDecorators = listOf(saveableDecorator, vmDecorator),
            entryProvider = entryProvider,
        )
    val settingsEntries =
        rememberDecoratedNavEntries(
            backStack = backStacks[SettingsRoute]!!,
            entryDecorators = listOf(saveableDecorator, vmDecorator),
            entryProvider = entryProvider,
        )

    return when (topLevelRoute) {
        SavedLocationsRoute -> savedLocationsEntries
        SettingsRoute -> settingsEntries
        else -> homeEntries
    }
}

class AppNavigator(
    private val state: AppNavigationState,
) {
    val navigationState: AppNavigationState get() = state

    fun navigate(route: NavKey) {
        if (state.isTabRoute(route)) {
            state.topLevelRoute = route
        } else {
            state.currentStack.add(route)
        }
    }

    fun goBack() {
        val stack = state.currentStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
        } else if (state.topLevelRoute != state.startRoute) {
            state.topLevelRoute = state.startRoute
        }
        // When on startRoute with a single entry, HomeScreen's BackHandler exits the app
    }
}
