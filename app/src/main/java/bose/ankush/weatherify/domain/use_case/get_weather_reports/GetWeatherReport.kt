package bose.ankush.weatherify.domain.use_case.get_weather_reports

import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.common.errorResponse
import bose.ankush.weatherify.data.room.weather.WeatherEntity
import bose.ankush.weatherify.domain.repository.WeatherRepository
import bose.ankush.weatherify.presentation.UIState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetWeatherReport @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(): Flow<UIState<WeatherEntity>> = flow {
        try {
            emit(UIState(isLoading = true))
            val response: WeatherEntity = repository.getWeatherReport().first()
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