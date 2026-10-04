package bose.ankush.home.presentation.shell

import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.LocalDate

internal enum class ShellTab(
    val label: String,
) {
    HOME("Home"),
    SAVED("Saved"),
    SETTINGS("Settings"),
}

internal data class ShellState(
    val tab: ShellTab = ShellTab.HOME,
    val lat: Double? = null,
    val lon: Double? = null,
    val placeName: String? = null,
    val featuredPlace: SavedLocation? = null,
    val eventDates: Set<LocalDate> = emptySet(),
    val photoUrl: String? = null,
    val showCreate: Boolean = false,
    val title: String = "",
    val dateText: String = "",
    val timeText: String = "",
    val posting: Boolean = false,
    val createError: String? = null,
)

internal sealed interface ShellIntent {
    data class SelectTab(
        val tab: ShellTab,
    ) : ShellIntent

    data object OpenAccount : ShellIntent

    data class LocationUpdated(
        val lat: Double,
        val lon: Double,
    ) : ShellIntent

    data class PlaceNameUpdated(
        val placeName: String?,
    ) : ShellIntent

    data object OpenCreate : ShellIntent

    data object DismissCreate : ShellIntent

    data class TitleChanged(
        val value: String,
    ) : ShellIntent

    data class DateChanged(
        val value: String,
    ) : ShellIntent

    data class TimeChanged(
        val value: String,
    ) : ShellIntent

    data class SideDataLoaded(
        val featured: SavedLocation?,
        val eventDates: Set<LocalDate>,
    ) : ShellIntent

    data class AccountLoaded(
        val photoUrl: String?,
    ) : ShellIntent

    data class CreateFailed(
        val message: String,
    ) : ShellIntent

    data object CreateSucceeded : ShellIntent
}

internal sealed interface ShellEffect {
    data object OpenSettings : ShellEffect
}

internal data class ShellReduce(
    val state: ShellState,
    val effect: ShellEffect? = null,
)

internal class ShellActions(
    val onIntent: (ShellIntent) -> Unit,
    val onSave: () -> Unit,
)
