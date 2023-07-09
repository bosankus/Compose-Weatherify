package bose.ankush.weatherify.presentation.cities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.domain.use_case.get_cities.GetCityNames
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CitiesViewModel @Inject constructor(
    getCityNames: GetCityNames
) : ViewModel() {

    var searchText = MutableStateFlow("")
        private set

    var isSearching = MutableStateFlow(false)
        private set

    private val cityNameList = MutableStateFlow(getCityNames())

    @OptIn(FlowPreview::class)
    val cityName: StateFlow<List<CityName>> = searchText
        .debounce(500L)
        .onEach { isSearching.update { true } }
        .combine(cityNameList) { text, city ->
            if (text.isBlank()) city
            else city.filter { it.doesMatchSearchQuery(text) }
        }
        .onEach { isSearching.update { false } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            cityNameList.value
        )

    fun onSearchTextChange(text: String) {
        searchText.value = text
    }
}