package bose.ankush.home.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSavedPlacesEntryImplTest {
    @Test
    fun requestsAreRefusedUntilAHomeWithAPlacesPageIsUp() {
        val entry = HomeSavedPlacesEntryImpl()
        assertFalse(entry.openSavedPlaces())
        assertFalse(entry.pending.value)

        entry.markAvailable()
        assertTrue(entry.openSavedPlaces())
        assertTrue(entry.pending.value)

        entry.consume()
        assertFalse(entry.pending.value)
    }
}
