package bose.ankush.home.data

import bose.ankush.home.HomeSavedPlacesEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds one pending "open saved places" request until the Wander places ViewModel takes it.
 * A request survives Home being off screen, so it is handled when Home is shown again.
 */
internal class HomeSavedPlacesEntryImpl : HomeSavedPlacesEntry {
    private val available = MutableStateFlow(false)
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    /** Called by the home that has a saved places page. Until then requests are refused. */
    fun markAvailable() {
        available.value = true
    }

    override fun openSavedPlaces(): Boolean {
        if (!available.value) return false
        _pending.value = true
        return true
    }

    fun consume() {
        _pending.value = false
    }
}
