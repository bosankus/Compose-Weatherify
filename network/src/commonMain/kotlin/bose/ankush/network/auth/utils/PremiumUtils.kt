package bose.ankush.network.auth.utils

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Returns true if the user's premium subscription is currently active.
 *
 * Derives premium status from [premiumExpiresAt] (ISO-8601 UTC string) at the point of
 * use rather than relying on the stored boolean flag, which may be stale between requests.
 */
fun isPremiumActive(premiumExpiresAt: String?): Boolean {
    if (premiumExpiresAt == null) return false
    return try {
        Clock.System.now() < Instant.parse(premiumExpiresAt)
    } catch (_: Exception) {
        false
    }
}
