package bose.ankush.weatherify.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.model.AvgForecast
import com.bosankus.utilities.DateTimeUtils
import java.util.*
import kotlin.math.roundToInt

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/

object Extension {

    fun Double.toCelsius(): String = (this - KELVIN_CONSTANT).roundToInt().toString()

    fun List<ForecastDto.ForecastList>.getForecastListForNext4Days():
            List<AvgForecast> {
        return filter { list -> (list.dt?.isNotMatchingWithTodayAndWithinNext4Days() == true) }
            .parseEachDayFromList()
    }

    private fun List<ForecastDto.ForecastList>.parseEachDayFromList(): List<AvgForecast> {
        val listOfAvgForecast = ArrayList<AvgForecast>()
        var avgTemp: Int
        var dayName: String
        var feelsLike: String?
        for (i in 1..4 step 1) {
            var totalTemp = 0
            var counter = 0
            for (j in this.indices step 1) {
                val date = this[j].dt
                if (date?.let { DateTimeUtils.getDayWiseDifferenceFromToday(it) } == i) {
                    val forecastObj = this[j]
                    feelsLike = forecastObj.main?.feelsLike?.toCelsius()
                    totalTemp += forecastObj.main?.temp?.toCelsius()?.toInt()!!
                    counter++
                    if ((counter % 7) == 0) {
                        avgTemp = totalTemp / counter
                        dayName = DateTimeUtils.getDayNameFromEpoch(date)
                        val avgForecast =
                            AvgForecast(this.hashCode(), date, dayName, "$avgTemp", feelsLike)
                        listOfAvgForecast.add(avgForecast)
                    }
                }
            }
        }
        return listOfAvgForecast
    }


    private fun Int.isNotMatchingWithTodayAndWithinNext4Days(): Boolean {
        val givenDate = Date(this.toLong() * 1000)
        val givenDateCalender = Calendar.getInstance()
        givenDateCalender.time = givenDate
        val givenYear = givenDateCalender.get(Calendar.YEAR)
        val currentYear = DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.YEAR)
        val givenDateNumber = givenDateCalender.get(Calendar.DAY_OF_MONTH + 1)
        val todayDateNumber =
            DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.DAY_OF_MONTH + 1)
        val differenceOfDate = DateTimeUtils.getDayWiseDifferenceFromToday(this)
        return (givenDateNumber > todayDateNumber && givenYear == currentYear && (differenceOfDate <= 4))
    }

    fun Context.openAppSystemSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        })
    }

}
