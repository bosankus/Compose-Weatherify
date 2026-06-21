@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.commonui.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun formatDate(
    millis: Long,
    pattern: String,
): String {
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    return formatter.stringFromDate(date)
}
