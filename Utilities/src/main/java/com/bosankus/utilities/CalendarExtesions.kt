package com.bosankus.utilities

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.*


object CalendarExtesions {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimestamp(): String = Instant.now().toEpochMilli().toString()


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


    fun getTodayDateInCalenderFormat(): Calendar {
        val todayDate = Date(System.currentTimeMillis())
        val calendarForToday = Calendar.getInstance()
        calendarForToday.time = todayDate
        return calendarForToday
    }

}