package bose.ankush.home.presentation.wander

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WanderPhotoPreferencesTest {
    @Test
    fun photoChoicePrefersLiveThenPersistedThenBundled() {
        assertEquals(
            "https://live",
            chooseWanderPhotoUrl(liveUrl = "https://live", persistedUrl = "https://persisted"),
        )
        assertEquals(
            "https://persisted",
            chooseWanderPhotoUrl(liveUrl = null, persistedUrl = "https://persisted"),
        )
        assertNull(
            chooseWanderPhotoUrl(liveUrl = null, persistedUrl = null),
            "null URL means Coil loads the bundled drawable",
        )
        assertTrue(WanderCondition.entries.all { it.key.isNotBlank() })
    }

    @Test
    fun bundledLookupOrderPutsConditionBeforeDefault() {
        assertEquals(
            listOf("wander_fog", "wander_default"),
            bundledDrawableNames(WanderCondition.FOG),
        )
    }
}
