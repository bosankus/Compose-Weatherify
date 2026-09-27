package bose.ankush.weatherify.base.logging

import bose.ankush.analytics.ErrorReporter
import timber.log.Timber

/**
 * Bridges Timber into [ErrorReporter] so Android-only code can keep logging normally without
 * creating a second route to Crashlytics — this deliberately does not touch the SDK itself.
 *
 * Routing is decided by whether a [Throwable] was passed, not by log level. Passing an exception
 * is the developer saying "something failed", and that intent shouldn't be silently discarded
 * because the call happens to sit at `Timber.d`. Filtering on level instead is how three
 * throwable-carrying call sites in this app stopped being reported without anyone noticing.
 *
 * Everything else becomes a breadcrumb: cheap, buffered locally, and only ever uploaded attached
 * to a real crash — which is what most `Timber.d` lines are actually good for.
 */
internal class CrashlyticsTree(
    private val errorReporter: ErrorReporter,
) : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (t != null) {
            errorReporter.recordError(t, message)
        } else {
            errorReporter.log(if (tag != null) "$tag: $message" else message)
        }
    }
}
