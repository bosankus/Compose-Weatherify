package com.bosankus.utilities

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Calendar
import java.util.Locale


object DateTimeUtils {

    fun getCurrentTimestamp(): Long = Instant.now().toEpochMilli()


    fun getDayWiseDifferenceFromToday(day: Int): Int {
        val todayDate = getTodayDateInCalenderFormat()
        val givenDate = Date(day.toLong() * 1000)
        val calenderForGivenDate = Calendar.getInstance()
        calenderForGivenDate.time = givenDate
        val givenDateNumber = calenderForGivenDate.get(Calendar.DAY_OF_MONTH + 1)
        val todayDateNumber = todayDate.get(Calendar.DAY_OF_MONTH + 1)
        return givenDateNumber - todayDateNumber
    }


    fun getDayNameFromEpoch(epoch: Int): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date(epoch.toLong() * 1000)
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            7 -> "Saturday"
            else -> "..."
        }
    }


    fun getTodayDateInCalenderFormat(): Calendar {
        val todayDate = Date(System.currentTimeMillis())
        val calendarForToday = Calendar.getInstance()
        calendarForToday.time = todayDate
        return calendarForToday
    }

    fun getTimeFromEpoch(epoch: Int?, zone: String = "Asia/Kolkata"): String {
        val format = "K:mm a"
        return epoch?.let {
            val zoneId = ZoneId.of(zone)
            val instant = Instant.ofEpochSecond(epoch.toLong())
            val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
            instant.atZone(zoneId).format(formatter)
        }.toString()
    }

    fun getFormattedDateTimeFromEpoch(epoch: Long?): String {
        epoch?.let {
            val instant = Instant.ofEpochSecond(it)
            val zoneId = ZoneId.systemDefault()

            // convert instant to local date time
            val localDateTime = LocalDateTime.ofInstant(instant, zoneId)

            // creating desired date time format
            val dateTimeFormat = DateTimeFormatter.ofPattern("EEE, dd MMM")

            return dateTimeFormat.format(localDateTime)
        } ?:
        return "Date & Time is unavailable at this moment"
    }
}