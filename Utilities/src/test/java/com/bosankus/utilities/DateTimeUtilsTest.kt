package com.bosankus.utilities

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*


class DateTimeUtilsTest {

    private val now = 1669873946L // 1st December 2022
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(now), ZoneId.systemDefault())

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(DateTimeUtils::class)
        mockkStatic(Clock::class)
        every { Clock.systemUTC() } returns fixedClock
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `verify that clock is fixed to given time`() {
        assertThat(Instant.now().toEpochMilli().toString()).isEqualTo("1669873946")
    }

    @Test
    fun `verify that getCurrentTimestamp returns time stamp successfully`() {
        val result = DateTimeUtils.getCurrentTimestamp()
        assertThat(result).isEqualTo(now.toString())
    }

    @Test
    fun getDayWiseDifferenceFromToday_WhenProvidedGivenDay_returnNumberOfDaysInBetween() {
        mockkStatic(Calendar::class)
        every { Calendar.getInstance().time = any() } returns Unit
        every { DateTimeUtils.getDayWiseDifferenceFromToday(now.toInt()) } returns 0
        val numberOfDays = DateTimeUtils.getDayWiseDifferenceFromToday(now.toInt())
        assertThat(numberOfDays).isEqualTo(0)
    }

    @Test
    fun getTodayDateInCalenderFormat_returnTodaysCorrectDateFormat() {
        val todaysDate = DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.YEAR)
        assertThat(todaysDate).isEqualTo(2022)
    }

    @Test
    fun getDayNameFromEpoch_WhenProvidedTodaysTimeStamp_returnCorrectDayName() {
        val dayName = DateTimeUtils.getDayNameFromEpoch(now.toInt())
        assertThat(dayName).isEqualTo("Thursday")
    }
}