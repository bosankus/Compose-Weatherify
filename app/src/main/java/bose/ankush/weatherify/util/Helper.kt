package bose.ankush.weatherify.util

import timber.log.Timber
import java.util.*

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/


object Helper {

    fun logMessage(message: String) = Timber.d(message)

    fun getTodayDateInCalenderFormat(): Calendar {
        val todayDate = Date(System.currentTimeMillis())
        val calendarForToday = Calendar.getInstance()
        calendarForToday.time = todayDate
        return calendarForToday
    }
}