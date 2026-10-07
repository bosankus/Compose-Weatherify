package bose.ankush.home.presentation.nearby

import kotlinx.datetime.LocalDate

/** "4 Oct". Day of month, then the three-letter month. */
internal fun LocalDate.toDayMonthLabel(): String {
    val month =
        month.name
            .take(MONTH_ABBREV)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
    return "$day $month"
}

private const val MONTH_ABBREV = 3
