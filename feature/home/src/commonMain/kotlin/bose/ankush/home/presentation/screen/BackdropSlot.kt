package bose.ankush.home.presentation.screen

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Part of the day the home photo is picked for, on the forecast place's own clock. [word] is
 * added to the Unsplash search, so an evening shows evening light.
 */
enum class DayPeriod(
    val key: String,
    val word: String,
) {
    MIDNIGHT("midnight", "midnight"),
    DAY("day", "morning"),
    AFTERNOON("afternoon", "afternoon"),
    EVENING("evening", "sunset"),
    NIGHT("night", "night"),
    ;

    companion object {
        fun ofHour(hour: Int): DayPeriod =
            when (hour) {
                in 0..4 -> MIDNIGHT
                in 5..11 -> DAY
                in 12..16 -> AFTERNOON
                in 17..19 -> EVENING
                else -> NIGHT
            }
    }
}

/**
 * The home backdrop's photo slot: one Unsplash pick per sky condition and part of the day,
 * changing every day. [query] is null until there is a forecast, and then nothing is searched.
 */
data class BackdropSlot(
    val condition: SkyCondition,
    val query: String?,
    val period: DayPeriod,
    /** The forecast place's local date, as days since 1970-01-01. */
    val epochDay: Int,
) {
    /** Where the slot's photo is persisted. The day is stored with it, so a pick from an earlier
     *  day still shows at once and is then replaced. */
    val key: String get() = "${condition.key}.${period.key}"

    /** Process cache key: the same search on the same day reuses its pick. */
    val cacheKey: String? get() = query?.let { "$it ${period.word}#$epochDay" }

    /** The Unsplash search, e.g. "rain landscape sunset". */
    val searchQuery: String? get() = query?.let { "$it ${period.word}" }

    /**
     * Today's photo from a search's results: a different one each day, the same all day. The
     * period is mixed in so neighbouring parts of the day don't step through results in lockstep.
     */
    fun <T> pick(results: List<T>): T? =
        results.takeIf { it.isNotEmpty() }
            ?.let { it[(epochDay + period.ordinal * PERIOD_STRIDE).mod(it.size)] }

    companion object {
        /** How many results a search asks for, so a month goes by before a photo repeats. */
        const val RESULTS_PER_SEARCH = 30
        private const val PERIOD_STRIDE = 7
    }
}

/** The slot for [now] at the forecast place ([timezoneOffset] seconds; null is the device zone). */
fun backdropSlot(
    condition: SkyCondition,
    weatherMain: String?,
    timezoneOffset: Int?,
    now: Instant,
): BackdropSlot {
    val local = now.toLocalDateTime(forecastZone(timezoneOffset))
    return BackdropSlot(
        condition = condition,
        query = weatherMain?.takeIf { it.isNotBlank() }?.let(::unsplashQuery),
        period = DayPeriod.ofHour(local.hour),
        epochDay = local.date.toEpochDays().toInt(),
    )
}

/**
 * The time now, then again at the top of every hour in [zone], when a part of the day can begin.
 * Hours are counted in [zone] so places on a half-hour offset turn over on time too.
 */
fun hourTicks(zone: TimeZone): Flow<Instant> =
    flow {
        while (true) {
            val now = Clock.System.now()
            emit(now)
            val local = now.toLocalDateTime(zone)
            val intoHourMs =
                (local.minute * 60L + local.second) * 1_000L + local.nanosecond / 1_000_000L
            delay(HOUR_MS - intoHourMs + TICK_SLACK_MS)
        }
    }

private const val HOUR_MS = 3_600_000L

/** Lands just after the hour, never just before it. */
private const val TICK_SLACK_MS = 500L
