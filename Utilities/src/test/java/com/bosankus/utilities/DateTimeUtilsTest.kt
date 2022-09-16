package com.bosankus.utilities

import com.bosankus.utilities.DateTimeUtils.getCurrentTimestamp
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class DateTimeUtilsTest {

    private lateinit var dateTimeUtils: DateTimeUtils
    private val epoch = 1669873946 // 1st December 2022

    @Before
    fun setup() {
        dateTimeUtils = mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getCurrentTimestamp_returnsTodaysTimeStampInEpoch() {
        val currentTimestamp = getCurrentTimestamp()
        assertThat(currentTimestamp).isNotEmpty()
    }

    @Test
    fun getDayWiseDifferenceFromToday_WhenProvidedGivenDay_returnNumberOfDaysInBetween() {
        val numberOfDays = DateTimeUtils.getDayWiseDifferenceFromToday(epoch)
        // asserting no. of days in between today & given epoch
        assertThat(numberOfDays).isEqualTo(123)
    }

    @Test
    fun getTodayDateInCalenderFormat_returnTodaysCorrectDateFormat() {
        val todayDate = DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.YEAR)
        // asserting correct year as per given epoch
        assertThat(todayDate).isEqualTo(2022)
    }

    @Test
    fun getDayNameFromEpoch_WhenProvidedTimeStamp_returnCorrectDayName() {
        val dayName = DateTimeUtils.getDayNameFromEpoch(epoch)
        // asserting correct day name as per given epoch
        assertThat(dayName).isEqualTo("Thursday")
    }
}
