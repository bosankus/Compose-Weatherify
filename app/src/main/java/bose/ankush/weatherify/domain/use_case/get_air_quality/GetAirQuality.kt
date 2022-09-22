package bose.ankush.weatherify.domain.use_case.get_air_quality

import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.toAirQuality
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetAirQuality @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(lat: Double, lang: Double): Flow<ResultData<AirQuality>> = flow {
        try {
            emit(ResultData.Loading)
            val response: AirQuality = weatherRepository.getAirQualityReport(lat.toString(), lang.toString()).toAirQuality()
            emit(ResultData.Success(response))
        } catch (e: HttpException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        } catch (e: IOException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        }
    }
}