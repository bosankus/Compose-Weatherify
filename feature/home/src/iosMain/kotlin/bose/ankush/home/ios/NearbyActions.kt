package bose.ankush.home.ios

import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyViewModel
import kotlinx.datetime.LocalDate

/** Taps on the nearby events block and the new-event sheet. */
class NearbyActions internal constructor(
    private val nearby: NearbyViewModel,
) {
    fun retryEvents() = nearby.onIntent(NearbyIntent.RetryEvents)

    /** Opens the form on the selected day, if one is selected. */
    fun openComposer() = nearby.onIntent(NearbyIntent.OpenComposer())

    /** A tapped day, `YYYY-MM-DD`: selects it and opens the form on it. */
    fun openComposerOn(date: String) = nearby.onIntent(NearbyIntent.OpenComposer(parseDay(date)))

    /** Narrows the events list to `YYYY-MM-DD`; an empty string shows every upcoming day. */
    fun selectDate(date: String) = nearby.onIntent(NearbyIntent.SelectDate(parseDay(date)))

    fun dismissComposer() = nearby.onIntent(NearbyIntent.DismissComposer)

    fun titleChanged(value: String) = nearby.onIntent(NearbyIntent.TitleChanged(value))

    /** `YYYY-MM-DD`. */
    fun dateChanged(value: String) = nearby.onIntent(NearbyIntent.DateChanged(value))

    /** `HH:mm`, 24-hour. */
    fun timeChanged(value: String) = nearby.onIntent(NearbyIntent.TimeChanged(value))

    fun noteChanged(value: String) = nearby.onIntent(NearbyIntent.NoteChanged(value))

    fun submit() = nearby.onIntent(NearbyIntent.SubmitEvent)

    private fun parseDay(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()
}
