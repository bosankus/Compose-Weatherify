package com.bosankus.utilities

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.*


object DateTimeUtilsImpl : DateTimeUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getCurrentTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Instant.now().toEpochMilli().toString()
        else (System.currentTimeMillis() / 1000).toString()
    }


    override fun getDayWiseDifferenceFromToday(day: Int): Int {
        val todayDate = getTodayDateInCalenderFormat()
        val givenDate = Date(day.toLong() * 1000)
        val calenderForGivenDate = Calendar.getInstance()
        calenderForGivenDate.time = givenDate
        val givenDateNumber = calenderForGivenDate.get(Calendar.DAY_OF_MONTH + 1)
        val todayDateNumber = todayDate.get(Calendar.DAY_OF_MONTH + 1)
        return givenDateNumber - todayDateNumber
    }


    override fun getTodayDateInCalenderFormat(): Calendar {
        val todayDate = Date(System.currentTimeMillis())
        val calendarForToday = Calendar.getInstance()
        calendarForToday.time = todayDate
        return calendarForToday
    }


    override fun getDayNameFromEpoch(epoch: Int): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date(epoch.toLong() * 1000)
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

}