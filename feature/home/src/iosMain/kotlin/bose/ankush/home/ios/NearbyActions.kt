package bose.ankush.home.ios

import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyViewModel

/** Taps on the nearby events block and the new-event sheet. */
class NearbyActions internal constructor(
    private val nearby: NearbyViewModel,
) {
    fun retryEvents() = nearby.onIntent(NearbyIntent.RetryEvents)

    fun openComposer() = nearby.onIntent(NearbyIntent.OpenComposer)

    fun dismissComposer() = nearby.onIntent(NearbyIntent.DismissComposer)

    fun titleChanged(value: String) = nearby.onIntent(NearbyIntent.TitleChanged(value))

    /** `YYYY-MM-DD`. */
    fun dateChanged(value: String) = nearby.onIntent(NearbyIntent.DateChanged(value))

    /** `HH:mm`, 24-hour. */
    fun timeChanged(value: String) = nearby.onIntent(NearbyIntent.TimeChanged(value))

    fun submit() = nearby.onIntent(NearbyIntent.SubmitEvent)
}
