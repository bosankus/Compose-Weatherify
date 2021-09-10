package bose.ankush.weatherify.util

import bose.ankush.weatherify.data.model.AvgForecast
import bose.ankush.weatherify.data.model.WeatherForecast
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/

object Extension {

    fun Double.toCelsius(): String = ((this - 32).div(1.8)).roundToInt().toString()

    fun List<WeatherForecast.ForecastList>.getForecastListForNext4Days():
            List<AvgForecast> {
        return filter { list -> (list.dt?.isNotMatchingWithTodayAndWithinNext4Days() == true) }
            .parseEachDayFromList()
    }

    fun Int.getDayName(): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date(this.toLong() * 1000)
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            1 -> "SUNDAY"
            2 -> "MONDAY"
            3 -> "TUESDAY"
            4 -> "WEDNESDAY"
            5 -> "THURSDAY"
            6 -> "FRIDAY"
            7 -> "SATURDAY"
            else -> "..."
        }
    }

    private fun List<WeatherForecast.ForecastList>.parseEachDayFromList(): List<AvgForecast> {
        val listOfAvgForecast = ArrayList<AvgForecast>()
        var avgTemp: Int
        var dayName: String
        for (i in 1..4 step 1) {
            var totalTemp = 0
            var counter = 0
            for (j in this.indices step 1) {
                val date = this[j].dt
                if (date?.findDayOfMonthWiseDifference() == i) {
                    val forecastObj = this[j]
                    totalTemp += forecastObj.main?.temp?.toCelsius()?.toInt()!!
                    counter++
                    if ((counter % 7) == 0) {
                        avgTemp = totalTemp / counter
                        dayName = date.getDayName()
                        val avgForecast = AvgForecast(this.hashCode(), dayName, "$avgTemp C")
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
        val currentYear = getTodayDateInCalenderFormat().get(Calendar.YEAR)
        val givenDateNumber = givenDateCalender.get(Calendar.DAY_OF_MONTH + 1)
        val todayDateNumber = getTodayDateInCalenderFormat().get(Calendar.DAY_OF_MONTH + 1)
        val differenceOfDate = this.findDayOfMonthWiseDifference()
        return (givenDateNumber > todayDateNumber && givenYear == currentYear && (differenceOfDate <= 4))
    }

    private fun Int.findDayOfMonthWiseDifference(todayDate: Calendar = getTodayDateInCalenderFormat()): Int {
        val givenDate = Date(this.toLong() * 1000)
        val calenderForGivenDate = Calendar.getInstance()
        calenderForGivenDate.time = givenDate
        val givenDateNumber = calenderForGivenDate.get(Calendar.DAY_OF_MONTH + 1)
        val todayDateNumber = todayDate.get(Calendar.DAY_OF_MONTH + 1)
        return givenDateNumber - todayDateNumber
    }

    private fun getTodayDateInCalenderFormat(): Calendar {
        val todayDate = Date(System.currentTimeMillis())
        val calendarForToday = Calendar.getInstance()
        calendarForToday.time = todayDate
        return calendarForToday
    }

}
