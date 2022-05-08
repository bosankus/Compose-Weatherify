package bose.ankush.weatherify.domain.use_case.get_weather_forecasts

import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.common.errorResponse
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetForecasts @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(): Flow<ResultData<ForecastDto>> = flow {
        try {
            emit(ResultData.Loading)
            val forecast: ForecastDto = repository.getWeatherForecastList()
            if (!forecast.list.isNullOrEmpty()) emit(ResultData.Success(forecast))
            else emit(ResultData.Failed(UiText.StringResource(R.string.empty_list)))
        } catch (e: HttpException) {
            val message = errorResponse(e.code())
            emit(ResultData.Failed(message))
        } catch (e: IOException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        }
    }
}