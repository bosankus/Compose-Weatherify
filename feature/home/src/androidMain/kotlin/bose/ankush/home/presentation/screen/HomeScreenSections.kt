package bose.ankush.home.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.photo.rememberAccountPhotoRequest
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_avatar
import bose.ankush.home.generated.resources.events_all_days
import bose.ankush.home.generated.resources.events_none_on_day
import bose.ankush.home.generated.resources.ic_sunny
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyState
import bose.ankush.home.presentation.nearby.toDayMonthLabel
import bose.ankush.home.presentation.util.SectionState
import bose.ankush.home.presentation.util.getIconUrl
import bose.ankush.home.presentation.util.valueOrNull
import bose.ankush.network.model.SavedLocation
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class FeaturedSavedPlace(
    val name: String,
    val subtitle: String,
    val message: String = "",
)

/**
 * Account photo, nearby events, and one saved place already loaded for this location.
 * Loading shows a shimmer and a failed call a shimmer with Retry; an empty list stays off
 * the column unless a day is selected. Nothing here is a placeholder title.
 */
data class NearbyContent(
    val photoUrl: String? = null,
    val showAccount: Boolean = false,
    val eventsLoading: Boolean = false,
    val eventsFailed: Boolean = false,
    val events: List<NearbyEventLine> = emptyList(),
    val savedPlace: FeaturedSavedPlace? = null,
    val selectedDate: LocalDate? = null,
    val onOpenAccount: () -> Unit = {},
    val onRetryEvents: () -> Unit = {},
    val onShowAllDays: () -> Unit = {},
) {
    val showEventsBlock: Boolean
        get() = eventsLoading || eventsFailed || events.isNotEmpty() || selectedDate != null

    val showBlocks: Boolean
        get() = showEventsBlock || savedPlace?.name?.isNotBlank() == true
}

/** The new-event form and where its intents go. */
internal class CreateEventBinding(
    val state: NearbyState = NearbyState(),
    val onIntent: (NearbyIntent) -> Unit = {},
)

/** Sections that sit beside the shell on the home screen. */
internal data class HomeScreenSections(
    val nearby: NearbyContent = NearbyContent(),
    val forecast: ForecastDetails = ForecastDetails(),
    val chrome: HomeScreenChrome = HomeScreenChrome(),
    val places: SavedPlacesBinding = SavedPlacesBinding(),
    val events: CreateEventBinding = CreateEventBinding(),
    val aiSummary: AiSummaryBinding = AiSummaryBinding(),
)

/** [days] is the daily forecast; each event line picks up its day's weather from it. */
internal fun NearbyState.toNearbyContent(
    photoUrl: String?,
    onOpenAccount: () -> Unit,
    days: List<CalendarStripDay> = emptyList(),
    onIntent: (NearbyIntent) -> Unit = {},
): NearbyContent =
    NearbyContent(
        photoUrl = photoUrl,
        showAccount = true,
        eventsLoading = events is SectionState.Loading,
        eventsFailed = events is SectionState.Failed,
        events = visibleEvents.toNearbyEventLines(days),
        savedPlace = featuredPlace.valueOrNull()?.toFeaturedSavedPlace(),
        selectedDate = selectedDate,
        onOpenAccount = onOpenAccount,
        onRetryEvents = { onIntent(NearbyIntent.RetryEvents) },
        onShowAllDays = { onIntent(NearbyIntent.SelectDate(null)) },
    )

private fun SavedLocation.toFeaturedSavedPlace(): FeaturedSavedPlace =
    FeaturedSavedPlace(
        name = name.trim(),
        subtitle = listOf(city, state, country).filter { it.isNotBlank() }.joinToString(", "),
    )

@Composable
internal fun AccountMark(
    photoUrl: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatar = painterResource(Res.drawable.default_avatar)
    Box(
        modifier =
            modifier
                .padding(top = 8.dp)
                .size(AccountMarkSize)
                .clip(CircleShape)
                .background(AccountMarkFill)
                .semantics {
                    role = Role.Button
                    contentDescription = ACCOUNT
                }.clickable(onClick = onOpen),
    ) {
        if (photoUrl.isNullOrBlank()) {
            Image(
                painter = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = rememberAccountPhotoRequest(photoUrl),
                placeholder = avatar,
                error = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun NearbyBlocks(
    nearby: NearbyContent,
    contentColor: Color,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!nearby.showEventsBlock && nearby.savedPlace == null) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (nearby.showEventsBlock) {
            NearbyEventsBlock(nearby = nearby, contentColor = contentColor)
        }
        nearby.savedPlace?.let { place ->
            FeaturedSavedPlaceCard(place = place, contentColor = contentColor, onOpen = onOpenSaved)
        }
    }
}

@Composable
private fun NearbyEventsBlock(
    nearby: NearbyContent,
    contentColor: Color,
) {
    val day = nearby.selectedDate?.toDayMonthLabel()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(cardFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics { contentDescription = NEARBY_EVENTS },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (day != null) "$NEARBY_EVENTS · $day" else NEARBY_EVENTS,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (day != null) {
                ActionLabel(
                    label = stringResource(Res.string.events_all_days),
                    onClick = nearby.onShowAllDays,
                    contentColor = contentColor,
                )
            }
        }
        when {
            nearby.eventsLoading || nearby.eventsFailed -> EventsShimmer(nearby, contentColor)
            nearby.events.isEmpty() && day != null ->
                Text(
                    text = stringResource(Res.string.events_none_on_day, day),
                    color = contentColor,
                    fontSize = 14.sp,
                )

            else -> nearby.events.forEach { event -> EventLine(event = event, contentColor = contentColor) }
        }
    }
}

/** Two shimmer bars while events load; a failed load keeps them and adds Retry. */
@Composable
private fun EventsShimmer(
    nearby: NearbyContent,
    contentColor: Color,
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier =
            Modifier
                .fillMaxWidth(PLACEHOLDER_WIDTH)
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush),
    )
    Box(
        modifier =
            Modifier
                .fillMaxWidth(0.32f)
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush),
    )
    if (nearby.eventsFailed) {
        ActionLabel(
            label = RetryLabels.EVENTS,
            onClick = nearby.onRetryEvents,
            contentColor = contentColor,
        )
    }
}

/** Title, then "4 Oct · 18:30", the note, and that day's forecast on the right. */
@Composable
private fun EventLine(
    event: NearbyEventLine,
    contentColor: Color,
) {
    val whenText = listOf(event.whenLabel, event.timeLabel).filter { it.isNotBlank() }.joinToString(" · ")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title.ifBlank { whenText },
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            if (event.title.isNotBlank()) {
                Text(text = whenText, color = contentColor, fontSize = 13.sp)
            }
            if (event.note.isNotBlank()) {
                Text(
                    text = event.note,
                    color = contentColor.copy(alpha = NOTE_ALPHA),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        event.weather?.let { weather -> EventWeather(weather = weather, contentColor = contentColor) }
    }
}

@Composable
private fun EventWeather(
    weather: EventDayWeather,
    contentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .padding(start = 8.dp)
                .semantics {
                    contentDescription =
                        listOf(weather.description, weather.range).filter { it.isNotBlank() }.joinToString(", ")
                },
    ) {
        if (weather.icon.isNotBlank()) {
            AsyncImage(
                model = weather.icon.getIconUrl(),
                placeholder = painterResource(Res.drawable.ic_sunny),
                error = painterResource(Res.drawable.ic_sunny),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp),
            )
        }
        if (weather.range.isNotBlank()) {
            Text(text = weather.range, color = contentColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FeaturedSavedPlaceCard(
    place: FeaturedSavedPlace,
    contentColor: Color,
    onOpen: () -> Unit,
) {
    val description = place.name.ifBlank { SAVED_PLACE }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardFill)
                .semantics {
                    role = Role.Button
                    contentDescription = description
                }.clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = SAVED_PLACE,
            color = contentColor,
            fontSize = 13.sp,
        )
        if (place.name.isNotBlank()) {
            Text(
                text = place.name,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (place.subtitle.isNotBlank()) {
            Text(
                text = place.subtitle,
                color = contentColor,
                fontSize = 14.sp,
            )
        }
        if (place.message.isNotBlank()) {
            Text(
                text = place.message,
                color = contentColor,
                fontSize = 14.sp,
            )
        }
    }
}

private val cardFill = Color.Black.copy(alpha = 0.38f)
private val AccountMarkSize = 40.dp

/** White disc behind the single-ink dog line art, so it reads on any background photo. */
private val AccountMarkFill = Color.White

private const val PLACEHOLDER_WIDTH = 0.46f
private const val NOTE_ALPHA = 0.8f
private const val ACCOUNT = "Account"
private const val NEARBY_EVENTS = "Nearby events"
private const val SAVED_PLACE = "Saved place"
