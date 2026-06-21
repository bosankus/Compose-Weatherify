package bose.ankush.weatherify.base

import bose.ankush.weatherify.base.DateTimeUtils.dayName
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateTimeUtilsTest {
    private val now = 1669873946L // 1st December 2022 (UTC)
    private lateinit var originalTimeZone: TimeZone

    /**
     * Initiate MockK and set a deterministic timezone before tests run
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(DateTimeUtils)
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /**
     * Restore timezone and unmock all objects after tests
     */
    @After
    fun teardown() {
        TimeZone.setDefault(originalTimeZone)
        unmockkAll()
    }

    /**
     * Verify that getDayWiseDifferenceFromToday can be stubbed and returns expected difference
     */
    @Test
    fun `verify that getDayWiseDifferenceFromToday returns day difference successfully`() {
        every { DateTimeUtils.getDayWiseDifferenceFromToday(now) } returns 0
        val numberOfDays = DateTimeUtils.getDayWiseDifferenceFromToday(now)
        assertThat(numberOfDays).isEqualTo(0)
    }

    /**
     * Verify that getTodayDateInCalenderFormat returns the current year
     */
    @Test
    fun `verify that getTodayDateInCalenderFormat returns correct year number`() {
        val todaysYear = DateTimeUtils.getTodayDateInCalenderFormat().get(Calendar.YEAR)
        val expectedYear = Calendar.getInstance().get(Calendar.YEAR)
        assertThat(todaysYear).isEqualTo(expectedYear)
    }

    /**
     * Verify getDayNameFromEpoch returns correct day name for the given epoch
     */
    @Test
    fun `verify that getDayNameFromEpoch returns correct day name`() {
        val dayName = now.dayName()
        assertThat(dayName).isEqualTo("Thursday")
    }
}
