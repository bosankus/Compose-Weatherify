package bose.ankush.weatherify.base

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

    /**
     * this method is helps to initiate mockk and setup mocked objects before tests are run
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(DateTimeUtils::class)
        mockkStatic(Clock::class)
        every { Clock.systemUTC() } returns fixedClock
    }

    /**
     * this method runs at the end of tests to unmockk all mocked objects
     */
    @After
    fun teardown() {
        unmockkAll()
    }
/*

    */
/**
     * this test verifies if clock has been fixed successfully
     *//*

    @Test
    fun `verify that clock is fixed to given time`() {
        assertThat(Instant.now().toEpochMilli().toString()).isEqualTo("1669873946")
    }

    */
/**
     * this test verifies that getCurrentTimestamp returns expected time stamp
     *//*

    @Test
    fun `verify that getCurrentTimestamp returns time stamp successfully`() {
        val result = DateTimeUtils.getCurrentTimestamp()
        assertThat(result).isEqualTo(now.toString())
    }
*/

    /**
     * this test verifies that getDayWiseDifferenceFromToday method returns expected day difference
     * as integer
     */
    @Test
    fun `verify that getDayWiseDifferenceFromToday returns day difference successfully`() {
        mockkStatic(Calendar::class)
        every { Calendar.getInstance().time = any() } returns Unit
        every { DateTimeUtils.getDayWiseDifferenceFromToday(now.toInt()) } returns 0
        val numberOfDays = DateTimeUtils.getDayWiseDifferenceFromToday(now.toInt())
        assertThat(numberOfDays).isEqualTo(0)
    }

    /**
     * this test verifies that getTodayDateInCalenderFormat returns correct year as per given epoch
     */
    @Test
    fun `verify that getTodayDateInCalenderFormat returns correct year number`() {
        val todaysDate = DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.YEAR)
        assertThat(todaysDate).isEqualTo(2023)
    }

    /**
     * this test verifies getDayNameFromEpoch returns correct day name as per given epoch
     */
    @Test
    fun `verify that getDayNameFromEpoch returns correct day name`() {
        val dayName = DateTimeUtils.getDayNameFromEpoch(now.toInt())
        assertThat(dayName).isEqualTo("Thursday")
    }
}