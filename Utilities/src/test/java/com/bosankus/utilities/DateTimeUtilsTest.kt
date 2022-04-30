package com.bosankus.utilities

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class DateTimeUtilsTest {

    private val epoch = 1669873946 // 1st December 2022

    @Test
    fun getCurrentTimestamp_returnsTodaysTimeStampInEpoch() {
        val currentTimestamp = DateTimeUtils.getCurrentTimestamp()
        assertThat(currentTimestamp).isNotEmpty()
    }

    @Test
    fun getDayWiseDifferenceFromToday_WhenProvidedGivenDay_returnNumberOfDaysInBetween() {
        val numberOfDays = DateTimeUtils.getDayWiseDifferenceFromToday(epoch)
        assertThat(numberOfDays).isEqualTo(216)
    }

    @Test
    fun getTodayDateInCalenderFormat_returnTodaysCorrectDateFormat() {
        val todaysDate = DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.YEAR)
        assertThat(todaysDate).isEqualTo(2022)
    }

    @Test
    fun getDayNameFromEpoch_WhenProvidedTodaysTimeStamp_returnCorrectDayName() {
        val dayName = DateTimeUtils.getDayNameFromEpoch(epoch)
        assertThat(dayName).isEqualTo("THURSDAY")
    }

}