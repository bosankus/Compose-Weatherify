package bose.ankush.analytics

/**
 * The single route from anywhere in the app to the crash reporter.
 *
 * Uncaught crashes are captured by the platform crash handler without any help from this; this
 * interface exists for the failures that a `catch` block absorbs so the UI can degrade gracefully,
 * which would otherwise leave no trace at all.
 *
 * Android code may keep using Timber — `CrashlyticsTree` forwards WARN+ into this same instance
 * rather than talking to the SDK itself, so there is still only one reporting path. Timber is an
 * Android `.aar` and cannot be referenced from `commonMain`, which is why shared code calls this
 * interface directly.
 */
interface ErrorReporter {
    /**
     * Files [throwable] as a non-fatal issue. Implementations must ignore cancellation, which is
     * normal coroutine control flow rather than a failure.
     *
     * @param context key/value pairs attached to the report, e.g. which endpoint or user action.
     */
    fun recordError(
        throwable: Throwable,
        message: String? = null,
        context: Map<String, String> = emptyMap(),
    )

    /** Breadcrumb attached to whatever fatal crash happens next. */
    fun log(message: String)

    /** Turns collection on or off — used to keep debug builds out of the release dashboard. */
    fun setEnabled(enabled: Boolean)

    /** Attaches a key to every subsequent report, e.g. build type or version. */
    fun setCustomKey(
        key: String,
        value: String,
    )
}
