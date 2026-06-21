package bose.ankush.commonui.util

expect fun formatDate(
    millis: Long,
    pattern: String = "MMM d, yyyy",
): String
