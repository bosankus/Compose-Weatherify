package bose.ankush.weatherify.base.common

import timber.log.Timber

/** Timber-backed [Logger] implementation for Android. */
class TimberLogger(private val tag: String) : Logger {
    override fun d(message: String) = Timber.tag(tag).d(message)
    override fun i(message: String) = Timber.tag(tag).i(message)
    override fun w(message: String) = Timber.tag(tag).w(message)
    override fun e(message: String, throwable: Throwable?) {
        if (throwable != null) Timber.tag(tag).e(throwable, message)
        else Timber.tag(tag).e(message)
    }
    override fun v(message: String) = Timber.tag(tag).v(message)
}

/** [LoggerFactory] that creates [TimberLogger] instances. */
class TimberLoggerFactory : LoggerFactory {
    override fun create(tag: String): Logger = TimberLogger(tag)
}
