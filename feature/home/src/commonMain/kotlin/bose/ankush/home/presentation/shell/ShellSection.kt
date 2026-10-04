package bose.ankush.home.presentation.shell

/**
 * Copy for a section that must stay on screen. [ShellSectionStatus.Ready] has no placeholder:
 * the live forecast, photo, week, or saved place is the content.
 */
internal fun placeholderMessage(
    kind: ShellSectionKind,
    status: ShellSectionStatus,
): String? =
    when (status) {
        ShellSectionStatus.Ready -> null
        ShellSectionStatus.Loading -> loadingCopy(kind)
        ShellSectionStatus.Empty -> emptyCopy(kind)
        ShellSectionStatus.Failed -> failedCopy(kind)
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
        ShellSectionKind.Calendar -> CALENDAR_LOADING
        ShellSectionKind.Events -> EVENTS_LOADING
        ShellSectionKind.SavedPlace -> SAVED_PLACE_LOADING
    }

private fun emptyCopy(kind: ShellSectionKind): String =
    when (kind) {
        ShellSectionKind.Forecast -> FORECAST_EMPTY
        ShellSectionKind.Photo -> PHOTO_EMPTY
        ShellSectionKind.Calendar -> CALENDAR_EMPTY
        ShellSectionKind.Events -> EVENTS_EMPTY
        ShellSectionKind.SavedPlace -> SAVED_PLACE_EMPTY
    }

private fun failedCopy(kind: ShellSectionKind): String =
    when (kind) {
        ShellSectionKind.Forecast -> FORECAST_FAILED
        ShellSectionKind.Photo -> PHOTO_FAILED
        ShellSectionKind.Calendar -> CALENDAR_FAILED
        ShellSectionKind.Events -> EVENTS_FAILED
        ShellSectionKind.SavedPlace -> SAVED_PLACE_FAILED
    }

internal const val SECTION_RETRY = "Retry"
internal const val FORECAST_LOADING = "Hold still. The sky has not reported in yet."
internal const val FORECAST_FAILED = "The forecast failed to load. The sky is not taking calls."
internal const val FORECAST_EMPTY = "The forecast came back empty. Nothing to put on this block."
internal const val PHOTO_LOADING = "Your photo has not walked in yet."
internal const val PHOTO_FAILED = "Your photo failed to load."
internal const val PHOTO_EMPTY = "No photo on this account. This spot stays put until there is one."
internal const val CALENDAR_LOADING = "The week has not loaded yet. The row stays right here."
internal const val CALENDAR_FAILED = "The week failed to load. This strip is not going anywhere."
internal const val CALENDAR_EMPTY = "The forecast brought no days. The calendar still holds its ground."
internal const val EVENTS_LOADING = "Nearby events have not loaded yet."
internal const val EVENTS_FAILED = "Nearby events failed to load."
internal const val EVENTS_EMPTY = "No events nearby. The calendar is staying up anyway."
internal const val SAVED_PLACE_LOADING = "A saved place within a kilometre has not loaded yet."
internal const val SAVED_PLACE_FAILED = "Saved places failed to load. This card is not leaving."
internal const val SAVED_PLACE_EMPTY = "No saved place within a kilometre. The card stays until one does."

private const val RETRY_FORECAST = "Retry forecast"
private const val RETRY_PHOTO = "Retry photo"
private const val RETRY_CALENDAR = "Retry calendar"
private const val RETRY_EVENTS = "Retry events"
private const val RETRY_SAVED_PLACE = "Retry saved place"
