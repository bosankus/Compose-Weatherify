package bose.ankush.home.presentation.shell

/**
 * Copy for a section that must stay on screen. [ShellSectionStatus.Ready] has no placeholder:
 * the live forecast, photo, or saved place is the content.
 *
 * The nearby-events block does not use a sentence. A loading or failed forecast week, and a
 * loading or failed GET /place-events call, are a shimmer of that block. A successful empty
 * week or event list stays quiet.
 */
internal fun placeholderMessage(
    kind: ShellSectionKind,
    status: ShellSectionStatus,
): String? {
    if (kind == ShellSectionKind.Calendar || kind == ShellSectionKind.Events) return null
    return when (status) {
        ShellSectionStatus.Ready -> null
        ShellSectionStatus.Loading -> loadingCopy(kind)
        ShellSectionStatus.Empty -> emptyCopy(kind)
        ShellSectionStatus.Failed -> failedCopy(kind)
    }
}

/** Day cells in the nearby-events shimmer. Same count as a loaded week row, not one bar. */
internal const val NEARBY_EVENTS_SHIMMER_DAY_COUNT = 7

/** Lines under the week that stand in for event content. They are not events. */
internal const val NEARBY_EVENTS_SHIMMER_LINE_COUNT = 2

internal enum class NearbyWeekBody {
    Days,
    Shimmer,
    Quiet,
}

/** Forecast days: the real week, a week-shaped shimmer, or a quiet empty success. */
internal fun nearbyWeekBody(dayStatus: ShellSectionStatus): NearbyWeekBody =
    when (dayStatus) {
        ShellSectionStatus.Ready -> NearbyWeekBody.Days
        ShellSectionStatus.Loading, ShellSectionStatus.Failed -> NearbyWeekBody.Shimmer
        ShellSectionStatus.Empty -> NearbyWeekBody.Quiet
    }

/** GET /place-events loading or failure is a shimmer, never an error sentence. */
internal fun nearbyEventsUseShimmer(status: ShellSectionStatus): Boolean =
    status == ShellSectionStatus.Loading || status == ShellSectionStatus.Failed

/**
 * Show the week skeleton when the forecast week is loading or failed, and also when events
 * are loading or failed and there is no real week on screen. That keeps the shimmer in the
 * shape of this block instead of a loose bar.
 */
internal fun showNearbyWeekShimmer(
    dayStatus: ShellSectionStatus,
    events: ShellSectionStatus,
    hasDays: Boolean,
): Boolean {
    val realWeek = nearbyWeekBody(dayStatus) == NearbyWeekBody.Days && hasDays
    return nearbyWeekBody(dayStatus) == NearbyWeekBody.Shimmer ||
        (nearbyEventsUseShimmer(events) && !realWeek)
}

internal fun retryContentDescription(kind: ShellSectionKind): String =
    when (kind) {
        ShellSectionKind.Forecast -> RETRY_FORECAST
        ShellSectionKind.Photo -> RETRY_PHOTO
        ShellSectionKind.Calendar -> RETRY_CALENDAR
        ShellSectionKind.Events -> RETRY_EVENTS
        ShellSectionKind.SavedPlace -> RETRY_SAVED_PLACE
    }

/**
 * Live current weather wins. A refresh that still has it stays [ShellSectionStatus.Ready].
 * Otherwise an in-flight load stays loading, a finished error stays failed, and a finished
 * call with nothing to show stays empty. None of those mean the block is removed.
 */
internal fun forecastSectionStatus(
    isLoading: Boolean,
    hasCurrent: Boolean,
    hasFailure: Boolean,
): ShellSectionStatus =
    when {
        hasCurrent -> ShellSectionStatus.Ready
        isLoading -> ShellSectionStatus.Loading
        hasFailure -> ShellSectionStatus.Failed
        else -> ShellSectionStatus.Empty
    }

/** Week cells come from the forecast. No days is empty, not a hidden strip. */
internal fun calendarDaysStatus(
    forecastStatus: ShellSectionStatus,
    dayCount: Int,
): ShellSectionStatus =
    when (forecastStatus) {
        ShellSectionStatus.Ready ->
            if (dayCount == 0) ShellSectionStatus.Empty else ShellSectionStatus.Ready
        else -> forecastStatus
    }

private fun loadingCopy(kind: ShellSectionKind): String =
    when (kind) {
        ShellSectionKind.Forecast -> FORECAST_LOADING
        ShellSectionKind.Photo -> PHOTO_LOADING
        ShellSectionKind.SavedPlace -> SAVED_PLACE_LOADING
        ShellSectionKind.Calendar, ShellSectionKind.Events -> ""
    }

private fun emptyCopy(kind: ShellSectionKind): String =
    when (kind) {
        ShellSectionKind.Forecast -> FORECAST_EMPTY
        ShellSectionKind.Photo -> PHOTO_EMPTY
        ShellSectionKind.SavedPlace -> SAVED_PLACE_EMPTY
        ShellSectionKind.Calendar, ShellSectionKind.Events -> ""
    }

private fun failedCopy(kind: ShellSectionKind): String =
    when (kind) {
        ShellSectionKind.Forecast -> FORECAST_FAILED
        ShellSectionKind.Photo -> PHOTO_FAILED
        ShellSectionKind.SavedPlace -> SAVED_PLACE_FAILED
        ShellSectionKind.Calendar, ShellSectionKind.Events -> ""
    }

internal const val SECTION_RETRY = "Retry"
internal const val FORECAST_LOADING = "Hold still. The sky has not reported in yet."
internal const val FORECAST_FAILED = "The forecast failed to load. The sky is not taking calls."
internal const val FORECAST_EMPTY = "The forecast came back empty. Nothing to put on this block."
internal const val PHOTO_LOADING = "Your photo has not walked in yet."
internal const val PHOTO_FAILED = "Your photo failed to load."
internal const val PHOTO_EMPTY = "No photo on this account. This spot stays put until there is one."
internal const val SAVED_PLACE_LOADING = "A saved place within a kilometre has not loaded yet."
internal const val SAVED_PLACE_FAILED = "Saved places failed to load. This card is not leaving."
internal const val SAVED_PLACE_EMPTY = "No saved place within a kilometre. The card stays until one does."

private const val RETRY_FORECAST = "Retry forecast"
private const val RETRY_PHOTO = "Retry photo"
private const val RETRY_CALENDAR = "Retry calendar"
private const val RETRY_EVENTS = "Retry events"
private const val RETRY_SAVED_PLACE = "Retry saved place"
