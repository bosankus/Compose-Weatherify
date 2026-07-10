package bose.ankush.settings.util

expect fun formatDate(
    millis: Long,
    pattern: String = "MMM d, yyyy",
): String
