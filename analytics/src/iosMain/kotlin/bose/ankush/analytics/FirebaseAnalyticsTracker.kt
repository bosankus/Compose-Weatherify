package bose.ankush.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal class FirebaseAnalyticsTracker : AnalyticsTracker {
    @Suppress("UNCHECKED_CAST")
    override fun track(event: AnalyticsEvent) {
        try {
            FIRAnalytics.logEventWithName(event.firebaseName, event.params as Map<Any?, *>)
        } catch (_: Exception) {
            // Analytics must never crash the app; drop the event on any SDK failure.
        }
    }
}
