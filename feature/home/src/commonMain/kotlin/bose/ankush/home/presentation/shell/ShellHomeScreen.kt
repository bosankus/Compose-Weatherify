package bose.ankush.home.presentation.shell

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_avatar
import bose.ankush.network.model.SavedLocation
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

/**
 * Type sizes come from [MaterialTheme.typography] so each line keeps that style's line height.
 * No font family is set here. No background photo is drawn.
 */
@Composable
internal fun ShellHomeScreen(
    state: ShellState,
    forecast: ShellForecast?,
    forecastStatus: ShellSectionStatus,
    actions: ShellActions,
    places: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientTop, GradientBottom))),
    ) {
        if (state.tab == ShellTab.SAVED) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { places() }
                ShellTabBar(
                    selected = state.tab,
                    onSelected = { actions.onIntent(ShellIntent.SelectTab(it)) },
                    modifier =
                        Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = ScreenHorizontal, vertical = 12.dp),
                )
            }
        } else {
            HomeColumn(state, forecast, forecastStatus, actions)
        }
        val canSave = canSubmitEvent(state)
        ShellCreateDialog(state = state, canSave = canSave, onIntent = actions.onIntent, onSave = actions.onSave)
    }
}

@Composable
private fun HomeColumn(
    state: ShellState,
    forecast: ShellForecast?,
    forecastStatus: ShellSectionStatus,
    actions: ShellActions,
) {
    val liveForecast = if (forecastStatus == ShellSectionStatus.Ready) forecast else null
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ScreenHorizontal)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Header(
                forecast = liveForecast,
                forecastStatus =
                    when {
                        liveForecast != null -> ShellSectionStatus.Ready
                        forecastStatus == ShellSectionStatus.Ready -> ShellSectionStatus.Empty
                        else -> forecastStatus
                    },
                photoStatus = state.accountPhoto,
                photoUrl = state.photoUrl,
                onAccount = { actions.onIntent(ShellIntent.OpenAccount) },
                onRetryForecast = actions.onRetryForecast,
            )
            Spacer(modifier = Modifier.height(28.dp))
            CalendarBlock(
                days = forecast?.days.orEmpty(),
                dayStatus = calendarDaysStatus(forecastStatus, forecast?.days?.size ?: 0),
                events = state.events,
                eventDates = state.eventDates,
                onPlus = { actions.onIntent(ShellIntent.OpenCreate) },
                onRetryForecast = actions.onRetryForecast,
                onRetryEvents = actions.onRetryEvents,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SavedPlaceSection(
                status = state.savedPlace,
                place = state.featuredPlace,
                onOpen = { actions.onIntent(ShellIntent.SelectTab(ShellTab.SAVED)) },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        ShellTabBar(
            selected = state.tab,
            onSelected = { actions.onIntent(ShellIntent.SelectTab(it)) },
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = ScreenHorizontal)
                    .padding(top = 4.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun Header(
    forecast: ShellForecast?,
    forecastStatus: ShellSectionStatus,
    photoStatus: ShellSectionStatus,
    photoUrl: String?,
    onAccount: () -> Unit,
    onRetryForecast: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sideBySide = maxWidth >= PhotoSize + HeaderGap + MinTitleWidth
        if (sideBySide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeaderGap),
                verticalAlignment = Alignment.Top,
            ) {
                ForecastTitle(
                    forecast = forecast,
                    forecastStatus = forecastStatus,
                    onRetryForecast = onRetryForecast,
                    modifier = Modifier.weight(1f),
                )
                AccountPhoto(
                    status = photoStatus,
                    photoUrl = photoUrl,
                    onAccount = onAccount,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HeaderGap),
            ) {
                ForecastTitle(
                    forecast = forecast,
                    forecastStatus = forecastStatus,
                    onRetryForecast = onRetryForecast,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    AccountPhoto(
                        status = photoStatus,
                        photoUrl = photoUrl,
                        onAccount = onAccount,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastTitle(
    forecast: ShellForecast?,
    forecastStatus: ShellSectionStatus,
    onRetryForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (forecast == null) {
        SectionHold(
            message = checkNotNull(placeholderMessage(ShellSectionKind.Forecast, forecastStatus)),
            retryDescription = retryContentDescription(ShellSectionKind.Forecast),
            onRetry = onRetryForecast,
            modifier = modifier,
        )
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (forecast.temperature.isNotEmpty()) {
                Text(
                    text = forecast.temperature,
                    color = Ink,
                    style = MaterialTheme.typography.displayLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (forecast.place.isNotEmpty()) {
                Text(
                    text = placeOnTwoLines(forecast.place),
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            WeatherFacts(forecast)
        }
    }
}

@Composable
private fun WeatherFacts(forecast: ShellForecast) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        forecast.sky?.let { sky ->
            Icon(
                imageVector = skyIcon(sky),
                contentDescription = forecast.conditionLine,
                tint = Ink,
                modifier = Modifier.size(28.dp),
            )
        }
        MetricIcon(icon = Icons.Filled.Thermostat, description = "Real feel", value = forecast.feel)
        MetricIcon(icon = Icons.Filled.Air, description = "Wind", value = forecast.wind)
        MetricIcon(icon = Icons.Filled.WbSunny, description = "UV", value = forecast.uv)
    }
}

private fun skyIcon(sky: ShellSky): ImageVector =
    when (sky) {
        ShellSky.Clear -> Icons.Filled.WbSunny
        ShellSky.Clouds -> Icons.Filled.Cloud
        ShellSky.Rain -> Icons.Filled.WaterDrop
        ShellSky.Snow -> Icons.Filled.AcUnit
        ShellSky.Thunderstorm -> Icons.Filled.Thunderstorm
        ShellSky.Mist -> Icons.Filled.Dehaze
    }

@Composable
private fun MetricIcon(
    icon: ImageVector,
    description: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Muted,
            modifier = Modifier.size(20.dp),
        )
        Text(text = value, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

/** City on its own line and country under it, so the place never crosses the temperature glyphs. */
private fun placeOnTwoLines(place: String): String {
    val parts = place.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 2) return place
    return parts.dropLast(1).joinToString(", ") + "\n" + parts.last()
}

@Composable
private fun AccountPhoto(
    status: ShellSectionStatus,
    photoUrl: String?,
    onAccount: () -> Unit,
) {
    val hasPhoto = status == ShellSectionStatus.Ready && !photoUrl.isNullOrBlank()
    val avatarPainter = painterResource(Res.drawable.default_avatar)
    Box(
        modifier =
            Modifier
                .size(PhotoSize)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(DayFill)
                .semantics {
                    role = Role.Button
                    contentDescription = "Account"
                }.clickable(onClick = onAccount),
    ) {
        if (hasPhoto) {
            val context = LocalPlatformContext.current
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(photoUrl)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.DISABLED)
                        .build(),
                placeholder = avatarPainter,
                error = avatarPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = avatarPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SectionHold(
    message: String,
    retryDescription: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onBodyClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .heightIn(min = 96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DayFill)
                .padding(start = 16.dp, top = 4.dp, end = 8.dp, bottom = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onRetry) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = retryDescription, tint = PlusYellow)
            }
        }
        Text(
            text = message,
            color = Ink,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                (
                    if (onBodyClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(onClick = onBodyClick)
                    }
                ).padding(top = 4.dp),
        )
    }
}

@Composable
private fun CalendarBlock(
    days: List<ShellDay>,
    dayStatus: ShellSectionStatus,
    events: ShellSectionStatus,
    eventDates: Set<LocalDate>,
    onPlus: () -> Unit,
    onRetryForecast: () -> Unit,
    onRetryEvents: () -> Unit,
) {
    val shownDayStatus =
        if (dayStatus == ShellSectionStatus.Ready && days.isEmpty()) {
            ShellSectionStatus.Empty
        } else {
            dayStatus
        }
    val showRealDays = nearbyWeekBody(shownDayStatus) == NearbyWeekBody.Days && days.isNotEmpty()
    val shimmerWeek = showNearbyWeekShimmer(shownDayStatus, events, hasDays = showRealDays)
    val shimmerEvents = nearbyEventsUseShimmer(events)
    val retryDays = dayStatus != ShellSectionStatus.Ready
    val retryEvents = events != ShellSectionStatus.Ready
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Event calendar",
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            if (retryDays || retryEvents) {
                val retryDescription =
                    listOfNotNull(
                        if (retryDays) retryContentDescription(ShellSectionKind.Calendar) else null,
                        if (retryEvents) retryContentDescription(ShellSectionKind.Events) else null,
                    ).joinToString(". ")
                IconButton(
                    onClick = {
                        if (retryDays) onRetryForecast()
                        if (retryEvents) onRetryEvents()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = retryDescription,
                        tint = PlusYellow,
                    )
                }
            }
            IconButton(onClick = onPlus) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(PlusYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add event", tint = Color(0xFF1A1A1A))
                }
            }
        }
        if (showRealDays || shimmerWeek || shimmerEvents) {
            val brush = if (shimmerWeek || shimmerEvents) rememberEventsShimmerBrush() else null
            if (showRealDays) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    days.forEach { day ->
                        DayCell(day = day, marked = day.date in eventDates)
                    }
                }
            } else if (shimmerWeek && brush != null) {
                Spacer(modifier = Modifier.height(12.dp))
                EventsWeekShimmer(brush = brush)
            }
            if (shimmerEvents && brush != null) {
                Spacer(modifier = Modifier.height(12.dp))
                EventsContentShimmer(brush = brush)
            }
        }
    }
}

@Composable
private fun rememberEventsShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "nearby-events")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "nearby-events-travel",
    )
    val base = Color.White.copy(alpha = 0.10f)
    val highlight = Color.White.copy(alpha = 0.34f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x = travel * SHIMMER_TRAVEL, y = 0f),
        end = Offset(x = travel * SHIMMER_TRAVEL + SHIMMER_SPAN, y = SHIMMER_DROP),
    )
}

/** Week-day row skeleton: a short label and a day circle, repeated across the week. */
@Composable
private fun EventsWeekShimmer(brush: Brush) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics { contentDescription = "Nearby events" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(NEARBY_EVENTS_SHIMMER_DAY_COUNT) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier
                            .size(width = ShimmerLabelWidth, height = ShimmerLabelHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(brush),
                )
            }
        }
    }
}

/** Event-content skeleton under the week. Widths follow a title and a shorter line, not copy. */
@Composable
private fun EventsContentShimmer(brush: Brush) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Nearby events" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(NEARBY_EVENTS_SHIMMER_LINE_COUNT) { index ->
            val fraction = if (index == 0) 0.72f else 0.46f
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush),
            )
        }
    }
}

@Composable
private fun DayCell(
    day: ShellDay,
    marked: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = day.label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (day.selected) Color.White else DayFill),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayOfMonth,
                color = if (day.selected) Color(0xFF161616) else Ink,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (marked) {
            Box(
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(PlusYellow),
            )
        }
    }
}

@Composable
private fun SavedPlaceSection(
    status: ShellSectionStatus,
    place: SavedLocation?,
    onOpen: () -> Unit,
) {
    if (status == ShellSectionStatus.Ready && place != null) {
        SavedPlaceCard(place = place, onClick = onOpen)
    } else {
        val shown = if (status == ShellSectionStatus.Ready) ShellSectionStatus.Empty else status
        SavedPlaceMessage(
            message = checkNotNull(placeholderMessage(ShellSectionKind.SavedPlace, shown)),
            onClick = onOpen,
        )
    }
}

@Composable
private fun SavedPlaceMessage(
    message: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DayFill)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = PlusYellow,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = message,
            color = Ink,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SavedPlaceCard(
    place: SavedLocation,
    onClick: () -> Unit,
) {
    val subtitle =
        listOf(place.city, place.state, place.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DayFill)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = PlusYellow,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (place.name.isNotBlank()) {
                Text(text = place.name, color = Ink, style = MaterialTheme.typography.titleMedium)
            }
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val ScreenHorizontal = 24.dp
private val HeaderGap = 16.dp
private val MinTitleWidth = 160.dp
private val PhotoSize = 56.dp
private val GradientTop = Color(0xFF3A342C)
private val GradientBottom = Color(0xFF14161C)
private val PlusYellow = Color(0xFFF5C400)
private val Ink = Color.White
private val Muted = Color.White.copy(alpha = 0.72f)
private val DayFill = Color(0x66101418)
private val ShimmerLabelWidth = 28.dp
private val ShimmerLabelHeight = 8.dp
private const val SHIMMER_TRAVEL = 700f
private const val SHIMMER_SPAN = 220f
private const val SHIMMER_DROP = 48f
