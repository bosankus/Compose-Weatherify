package bose.ankush.home.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackgroundPhotoPreferencesTest {
    @Test
    fun photoChoicePrefersLiveThenPersistedThenBundled() {
        assertEquals(
            "https://live",
            chooseBackgroundPhotoUrl(liveUrl = "https://live", persistedUrl = "https://persisted"),
        )
        assertEquals(
            "https://persisted",
            chooseBackgroundPhotoUrl(liveUrl = null, persistedUrl = "https://persisted"),
        )
        assertNull(
            chooseBackgroundPhotoUrl(liveUrl = null, persistedUrl = null),
            "null URL means Coil loads the bundled drawable",
        )
        assertTrue(SkyCondition.entries.all { it.key.isNotBlank() })
    }

    @Test
    fun bundledLookupOrderPutsConditionBeforeDefault() {
        assertEquals(
            listOf("background_fog", "background_default"),
            bundledDrawableNames(SkyCondition.FOG),
        )
    }
}
