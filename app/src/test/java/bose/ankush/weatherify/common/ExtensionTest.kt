package bose.ankush.weatherify.common

import bose.ankush.weatherify.base.common.Extension
import bose.ankush.weatherify.base.common.Extension.toCelsius
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExtensionTest {

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(Extension::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun toCelsius_WhenKelvinTempProvided_ReturnsCelsiusString() {
        val kelvinTemp = 289.12
        val celsiusTemp = kelvinTemp.toCelsius()
        assertThat(celsiusTemp).isEqualTo("16")
    }
}