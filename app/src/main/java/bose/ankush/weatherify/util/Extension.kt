package bose.ankush.weatherify.util

import bose.ankush.weatherify.model.AvgForecast
import bose.ankush.weatherify.model.WeatherForecast
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/

fun Double.toCelsius(): String = (this - KELVIN_CONSTANT).roundToInt().toString()


fun List<WeatherForecast.ForecastList>.getForecastListForNext4Days():
        List<AvgForecast> {
    return filter { list -> (list.dt?.isNotMatchingWithTodayAndWithinNext4Days() == true) }
        .parseEachDayFromList()
}


fun List<WeatherForecast.ForecastList>.parseEachDayFromList(): List<AvgForecast> {

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


fun Int.findDayOfMonthWiseDifference(): Int {
    // get the Date of both today's and provided epoch
    val givenDate = Date(this.toLong() * 1000)
    val todayDate = Date(System.currentTimeMillis())

    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance()

    cal1.time = givenDate
    cal2.time = todayDate

    // Finding difference of the dates to limit data till 4th day
    val givenDateNumber = cal1.get(Calendar.DAY_OF_MONTH + 1)
    val todayDateNumber = cal2.get(Calendar.DAY_OF_MONTH + 1)

    return givenDateNumber - todayDateNumber
}


fun Int.isNotMatchingWithTodayAndWithinNext4Days(): Boolean {
    val givenDate = Date(this.toLong() * 1000)
    val todayDate = Date(System.currentTimeMillis())

    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance()

    cal1.time = givenDate
    cal2.time = todayDate

    val givenYear = cal1.get(Calendar.YEAR)
    val currentYear = cal2.get(Calendar.YEAR)

    // Finding difference of the dates to limit data till 4th day
    val givenDateNumber = cal1.get(Calendar.DAY_OF_MONTH + 1)
    val todayDateNumber = cal2.get(Calendar.DAY_OF_MONTH + 1)

    val differenceOfDate = givenDateNumber - todayDateNumber

    return (givenDateNumber > todayDateNumber && givenYear == currentYear && (differenceOfDate <= 4))
}


fun Int.getDayName(): String {
    val calendar = Calendar.getInstance();
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
