package bose.ankush.auth.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Platform-agnostic interface for device and app metadata used during registration.
 * Provided per-platform via `authDomainModule` in androidMain/iosMain.
 */
interface DeviceInfoProvider {
    fun getDeviceModel(): String

    fun getOperatingSystem(): String

    fun getOsVersion(): String

    fun getAppVersion(): String

    fun getRegistrationSource(): String

    fun getIpAddress(): String?

    fun getCurrentUtcTimestamp(): String

    suspend fun getFirebaseToken(): String?
}

/** ISO-8601 UTC timestamp shared by platform [DeviceInfoProvider] implementations. */
internal fun currentUtcTimestamp(): String {
    val now = Clock.System.now()
    val dateTime = now.toLocalDateTime(TimeZone.UTC)
    val millis = dateTime.nanosecond / 1_000_000
    return "${dateTime.year.pad(4)}-${dateTime.month.number.pad(2)}-${dateTime.day.pad(2)}T" +
        "${dateTime.hour.pad(2)}:${dateTime.minute.pad(2)}:${dateTime.second.pad(2)}.${millis.pad(3)}Z"
}

private fun Int.pad(width: Int) = toString().padStart(width, '0')
