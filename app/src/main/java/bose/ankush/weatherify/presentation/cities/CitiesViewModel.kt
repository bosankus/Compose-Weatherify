package bose.ankush.weatherify.presentation.cities

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.domain.use_case.get_cities.GetCityNames
import bose.ankush.weatherify.presentation.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CitiesViewModel @Inject constructor(
    getCityNames: GetCityNames
) : ViewModel() {

    private val _cityNameState = mutableStateOf(UIState<List<CityName>>())
    val cityNameState: State<UIState<List<CityName>>> = _cityNameState

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _cityName = MutableStateFlow(getCityNames())
    @OptIn(FlowPreview::class)
    val cityName: StateFlow<List<CityName>> = searchText
        .debounce(500L)
        .onEach { _isSearching.update { true } }
        .combine(_cityName) { text, cityName ->
            if (text.isBlank()) cityName
            else cityName.filter { it.doesMatchSearchQuery(text) }
        }
        .onEach { _isSearching.update { false } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _cityName.value
        )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }
}