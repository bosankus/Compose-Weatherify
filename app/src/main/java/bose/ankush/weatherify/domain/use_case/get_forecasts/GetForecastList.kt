package bose.ankush.weatherify.domain.use_case.get_forecasts

import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.repository.WeatherRepository
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetForecastList @Inject constructor(
    private val repository: WeatherRepository
) {

    operator fun invoke(): Flow<ResultData<List<ForecastDto.ForecastList>>> = flow {
        try {
            emit(ResultData.Loading)
            val forecastDtoList: List<ForecastDto.ForecastList> = repository.getWeatherForecastList()
            emit(ResultData.Success(forecastDtoList))
        } catch (e: HttpException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        } catch (e: IOException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        }
    }
}