package bose.ankush.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
