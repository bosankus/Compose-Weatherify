package com.bosankus.utilities

import java.util.*

interface DateTimeUtils {

    fun getCurrentTimestamp(): String

    fun getDayWiseDifferenceFromToday(day: Int): Int

    fun getTodayDateInCalenderFormat(): Calendar

    fun getDayNameFromEpoch(epoch: Int): String
}