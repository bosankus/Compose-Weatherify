package bose.ankush.weatherify.base

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Singleton class to provide utility values related to date and time throughout all the modules.
 */
object DateTimeUtils {
    /**
     * Returns numbers of days between today and given time on argument
     */
    fun getDayWiseDifferenceFromToday(day: Long): Int {
        val todayDate = getTodayDateInCalenderFormat()
        val givenDate = Date(day * 1000)
        val calenderForGivenDate = Calendar.getInstance()
        calenderForGivenDate.time = givenDate
        val givenDateNumber = calenderForGivenDate.get(Calendar.DAY_OF_MONTH)
        val todayDateNumber = todayDate.get(Calendar.DAY_OF_MONTH)
        return givenDateNumber - todayDateNumber
    }

    /**
     * Returns name of the day from given epoch. Epoch to be provided in Integer format
     * via argument
     */
    fun Long.dayName(): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date(this * 1000)
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

    /**
     * Returns current date in Calender type
     */
    fun getTodayDateInCalenderFormat(): Calendar {
        val todayDate = Date(System.currentTimeMillis())
        val calendarForToday = Calendar.getInstance()
        calendarForToday.time = todayDate
        return calendarForToday
    }

    fun Long.toFormattedTime(zone: String = "Asia/Kolkata"): String {
        val format = "K:mm a"
        val zoneId = ZoneId.of(zone)
        val instant = Instant.ofEpochSecond(this)
        val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
        return instant.atZone(zoneId).format(formatter)
    }

    fun getFormattedDateTimeFromEpoch(epoch: Long?): String {
        epoch ?: return "Date & Time is unavailable at this moment"
        val instant = Instant.ofEpochSecond(epoch)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return DateTimeFormatter.ofPattern("EEE, dd MMM").format(localDateTime)
    }
}
