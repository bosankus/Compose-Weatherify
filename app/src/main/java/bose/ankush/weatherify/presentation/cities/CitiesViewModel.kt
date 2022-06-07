package bose.ankush.weatherify.presentation.cities

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.domain.use_case.get_cities.GetCityNames
import bose.ankush.weatherify.presentation.cities.state.CityNameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CitiesViewModel @Inject constructor(
    getCityNames: GetCityNames
) : ViewModel() {

    private val _cityNameState = mutableStateOf(CityNameState())
    val cityNameState: State<CityNameState> = _cityNameState

    init {
        getCityNames().onEach { result ->
            when (result) {
                is ResultData.DoNothing -> {}
                is ResultData.Loading -> _cityNameState.value = CityNameState(isLoading = true)
                is ResultData.Success -> _cityNameState.value =
                    CityNameState(names = result.data ?: emptyList())
                is ResultData.Failed -> _cityNameState.value =
                    CityNameState(error = UiText.DynamicText(result.message.toString()))
            }
        }.launchIn(viewModelScope)
    }
}