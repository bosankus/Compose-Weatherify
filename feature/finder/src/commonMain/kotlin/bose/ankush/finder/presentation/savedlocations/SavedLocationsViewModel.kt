package bose.ankush.finder.presentation.savedlocations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.finder.domain.usecase.DeleteLocationUseCase
import bose.ankush.finder.domain.usecase.GetSavedLocationsUseCase
import bose.ankush.finder.domain.usecase.SaveLocationParams
import bose.ankush.finder.domain.usecase.SaveLocationUseCase
import bose.ankush.finder.generated.resources.Res
import bose.ankush.finder.generated.resources.saved_locations_delete_error
import bose.ankush.finder.generated.resources.saved_locations_delete_success
import bose.ankush.finder.generated.resources.saved_locations_load_error
import bose.ankush.finder.generated.resources.saved_locations_save_error
import bose.ankush.finder.generated.resources.saved_locations_save_success
import bose.ankush.payment.domain.store.PremiumStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal class SavedLocationsViewModel(
    private val getSavedLocationsUseCase: GetSavedLocationsUseCase,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val deleteLocationUseCase: DeleteLocationUseCase,
    private val premiumStore: PremiumStore,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(SavedLocationsState())
    val state: StateFlow<SavedLocationsState> = _state.asStateFlow()

    private val _effect = Channel<SavedLocationsEffect>(Channel.BUFFERED)
    val effect: Flow<SavedLocationsEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            premiumStore.observePremiumStatus().collect { status ->
                val wasPremium = _state.value.isPremium
                _state.update { it.copy(isPremium = status.isPremium) }
                if (status.isPremium && !wasPremium) loadSavedLocations()
            }
        }
    }

    fun processIntent(intent: SavedLocationsIntent) {
        when (intent) {
            is SavedLocationsIntent.Load -> loadSavedLocations()
            is SavedLocationsIntent.Save -> saveLocation(intent.name, intent.lat, intent.lon)
            is SavedLocationsIntent.Delete -> deleteLocation(intent.id)
            is SavedLocationsIntent.SelectLocation ->
                emitLocationSelected(
                    intent.location.lat,
                    intent.location.lon,
                    intent.location.name,
                )
            is SavedLocationsIntent.MessageShown -> clearMessage()
        }
    }

    private fun loadSavedLocations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getSavedLocationsUseCase().fold(
                onSuccess = { locations ->
                    _state.update { it.copy(isLoading = false, locations = locations) }
                },
                onFailure = { e ->
                    if (e !is CancellationException) {
                        val message = getString(Res.string.saved_locations_load_error)
                        _state.update { it.copy(isLoading = false, error = message) }
                    }
                },
            )
        }
    }

    private fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            saveLocationUseCase(SaveLocationParams(name, lat, lon)).fold(
                onSuccess = {
                    analyticsTracker.track(AnalyticsEvent.LocationSaved("saved_locations_screen"))
                    val message = getString(Res.string.saved_locations_save_success)
                    _state.update { it.copy(isLoading = false, successMessage = message) }
                    loadSavedLocations()
                },
                onFailure = { e ->
                    if (e !is CancellationException) {
                        val message = getString(Res.string.saved_locations_save_error)
                        _state.update { it.copy(isLoading = false, error = message) }
                    }
                },
            )
        }
    }

    private fun deleteLocation(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            deleteLocationUseCase(id).fold(
                onSuccess = {
                    val message = getString(Res.string.saved_locations_delete_success)
                    _state.update { it.copy(isLoading = false, successMessage = message) }
                    loadSavedLocations()
                },
                onFailure = { e ->
                    if (e !is CancellationException) {
                        val message = getString(Res.string.saved_locations_delete_error)
                        _state.update { it.copy(isLoading = false, error = message) }
                    }
                },
            )
        }
    }

    private fun clearMessage() {
        _state.update { it.copy(error = null, successMessage = null) }
    }

    private fun emitLocationSelected(
        lat: Double,
        lon: Double,
        name: String,
    ) {
        analyticsTracker.track(AnalyticsEvent.LocationSelected("saved_locations_list"))
        _effect.trySend(SavedLocationsEffect.LocationSelected(lat, lon, name))
    }
}
