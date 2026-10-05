package bose.ankush.home.presentation.wander

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_avatar
import bose.ankush.home.presentation.shell.ShellEventSummary
import bose.ankush.home.presentation.shell.ShellSectionKind
import bose.ankush.home.presentation.shell.ShellSectionStatus
import bose.ankush.home.presentation.shell.ShellState
import bose.ankush.home.presentation.shell.placeholderMessage
import bose.ankush.home.presentation.shell.retryContentDescription
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import org.jetbrains.compose.resources.painterResource

data class WanderEventLine(
    val title: String,
    val whenLabel: String,
)

data class WanderSavedPlace(
    val name: String,
    val subtitle: String,
    val message: String = "",
)

/**
 * Account photo, nearby events, and one saved place already loaded for this location.
 * Empty and failed calls stay off the column. Nothing here is a placeholder title.
 */
data class WanderNearby(
    val photoUrl: String? = null,
    val showAccount: Boolean = false,
    val eventsLoading: Boolean = false,
    val eventsFailed: Boolean = false,
    val events: List<WanderEventLine> = emptyList(),
    val savedPlace: WanderSavedPlace? = null,
    val onOpenAccount: () -> Unit = {},
    val onRetryEvents: () -> Unit = {},
)

/** Sections that sit beside the shell on Wander home. */
data class WanderHomeSections(
    val nearby: WanderNearby = WanderNearby(),
    val forecast: WanderForecastDetails = WanderForecastDetails(),
    val chrome: WanderHomeChrome = WanderHomeChrome(),
)

internal fun ShellState.toWanderNearby(
    onOpenAccount: () -> Unit,
    onRetryEvents: () -> Unit = {},
): WanderNearby {
    val place = featuredPlace
    val subtitle =
        place
            ?.let { saved ->
                listOf(saved.city, saved.state, saved.country)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
            }.orEmpty()
    return WanderNearby(
        photoUrl = photoUrl?.takeIf { it.isNotBlank() },
        showAccount = true,
        eventsLoading = events == ShellSectionStatus.Loading,
        eventsFailed = events == ShellSectionStatus.Failed,
        events =
            if (events == ShellSectionStatus.Ready) {
                eventSummaries.map { it.toWanderLine() }
            } else {
                emptyList()
            },
        savedPlace =
            place?.let {
                WanderSavedPlace(
                    name = it.name.trim(),
                    subtitle = subtitle,
                    message = "",
                )
            },
        onOpenAccount = onOpenAccount,
        onRetryEvents = onRetryEvents,
    )
}

private fun ShellEventSummary.toWanderLine(): WanderEventLine = WanderEventLine(title = title, whenLabel = whenLabel)

@Composable
internal fun WanderAccountMark(
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
internal fun WanderNearbyBlocks(
    nearby: WanderNearby,
    contentColor: Color,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showEvents = nearby.eventsLoading || nearby.eventsFailed || nearby.events.isNotEmpty()
    if (!showEvents && nearby.savedPlace == null) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showEvents) {
            WanderEventsBlock(
                loading = nearby.eventsLoading || nearby.eventsFailed,
                failed = nearby.eventsFailed,
                events = nearby.events,
                contentColor = contentColor,
                onRetry = nearby.onRetryEvents,
            )
        }
        nearby.savedPlace?.let { place ->
            WanderSavedPlaceCard(place = place, contentColor = contentColor, onOpen = onOpenSaved)
        }
    }
}

@Composable
private fun WanderEventsBlock(
    loading: Boolean,
    failed: Boolean,
    events: List<WanderEventLine>,
    contentColor: Color,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(cardFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics { contentDescription = NEARBY_EVENTS },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = NEARBY_EVENTS,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        if (loading) {
            val brush = rememberWanderShimmerBrush()
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
            if (failed) {
                WanderActionLabel(
                    label = retryContentDescription(ShellSectionKind.Events),
                    onClick = onRetry,
                    contentColor = contentColor,
                )
            }
        } else {
            events.forEach { event ->
                EventLine(event = event, contentColor = contentColor)
            }
        }
    }
}

@Composable
private fun EventLine(
    event: WanderEventLine,
    contentColor: Color,
) {
    val primary = event.title.ifBlank { event.whenLabel }
    Text(
        text = primary,
        color = contentColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
    )
    if (event.title.isNotBlank()) {
        Text(
            text = event.whenLabel,
            color = contentColor,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun WanderSavedPlaceCard(
    place: WanderSavedPlace,
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

/** White disc behind the single-ink dog line art, so it reads on any Wander photo. */
private val AccountMarkFill = Color.White

private const val PLACEHOLDER_WIDTH = 0.46f
private const val ACCOUNT = "Account"
private const val NEARBY_EVENTS = "Nearby events"
private const val SAVED_PLACE = "Saved place"
