package bose.ankush.language.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageMatchTest {
    private val languages = listOf("en-IN", "hi-IN", "iw-IL", "kn-IN")

    @Test
    fun anExactTagWins() {
        assertEquals("hi-IN", matchLanguage(languages, "hi-IN"))
        assertEquals("en-IN", matchLanguage(languages, "en_in"))
    }

    @Test
    fun theDeviceRegionFallsBackToTheSameLanguage() {
        assertEquals("en-IN", matchLanguage(languages, "en-US"))
    }

    @Test
    fun legacyAndModernHebrewCodesMatch() {
        assertEquals("iw-IL", matchLanguage(languages, "he-IL"))
        assertEquals("iw-IL", matchLanguage(languages, "he"))
    }

    @Test
    fun anUnlistedLanguageMatchesNothing() {
        assertNull(matchLanguage(languages, "fr-FR"))
        assertNull(matchLanguage(languages, ""))
    }
}
