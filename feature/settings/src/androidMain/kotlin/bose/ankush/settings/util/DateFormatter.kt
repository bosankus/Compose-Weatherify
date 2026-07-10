package bose.ankush.settings.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatDate(
    millis: Long,
    pattern: String,
): String {
    val df = SimpleDateFormat(pattern, Locale.getDefault())
    return df.format(Date(millis))
}
