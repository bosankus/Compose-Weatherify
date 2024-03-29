package bose.ankush.weatherify.domain.use_case.get_air_quality

import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.common.errorResponse
import bose.ankush.weatherify.data.remote.dto.toAirQuality
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.repository.WeatherRepository
import bose.ankush.weatherify.presentation.UIState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetAirQuality @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(lat: Double, lang: Double): Flow<UIState<AirQuality>> = flow {
        try {
            emit(UIState(isLoading = true))
            val response: AirQuality = weatherRepository.getAirQualityReport(lat.toString(), lang.toString()).toAirQuality()
            emit(UIState(isLoading = false, data = response, error = null))
        } catch (e: HttpException) {
            val message = errorResponse(e.code())
            emit(
                UIState(
                    isLoading = false,
                    data = null,
                    error = UiText.DynamicText(message.toString())
                )
            )
        } catch (e: IOException) {
            emit(
                UIState(
                    isLoading = false,
                    data = null,
                    error = UiText.DynamicText(e.message.toString())
                )
            )
        }
    }
}