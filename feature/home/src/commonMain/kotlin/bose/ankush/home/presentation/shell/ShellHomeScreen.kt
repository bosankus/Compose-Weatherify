package bose.ankush.home.presentation.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.network.model.SavedLocation
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate

/**
 * Layout stand-ins. The poster does not yield a measured dp, hex, radius, shadow, or type size.
 * No font is set here. No background photo is drawn.
 */
@Composable
internal fun ShellHomeScreen(
    state: ShellState,
    forecast: ShellForecast?,
    statusMessage: String?,
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
            HomeColumn(state, forecast, statusMessage, actions)
        }
        val canSave = canSubmitEvent(state)
        ShellCreateDialog(state = state, canSave = canSave, onIntent = actions.onIntent, onSave = actions.onSave)
    }
}

@Composable
private fun HomeColumn(
    state: ShellState,
    forecast: ShellForecast?,
    statusMessage: String?,
    actions: ShellActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = ScreenHorizontal)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Header(
            forecast = forecast,
            photoUrl = state.photoUrl,
            onAccount = { actions.onIntent(ShellIntent.OpenAccount) },
        )
        if (forecast == null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = statusMessage ?: "Loading", color = Ink, fontSize = 16.sp)
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = forecast.conditionLine, color = Ink, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MetricLine(label = "Real feel", value = forecast.feel)
            MetricLine(label = "Wind", value = forecast.wind)
            MetricLine(label = "UV", value = forecast.uv)
            Spacer(modifier = Modifier.height(20.dp))
            CalendarBlock(
                days = forecast.days,
                eventDates = state.eventDates,
                onPlus = { actions.onIntent(ShellIntent.OpenCreate) },
            )
            state.featuredPlace?.let { place ->
                Spacer(modifier = Modifier.height(16.dp))
                SavedPlaceCard(place = place, onClick = { actions.onIntent(ShellIntent.SelectTab(ShellTab.SAVED)) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ShellTabBar(selected = state.tab, onSelected = { actions.onIntent(ShellIntent.SelectTab(it)) })
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun Header(
    forecast: ShellForecast?,
    photoUrl: String?,
    onAccount: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = forecast?.temperatureLine ?: MISSING,
            color = Ink,
            fontSize = 40.sp,
            modifier = Modifier.weight(1f),
        )
        AccountSpot(photoUrl = photoUrl, onClick = onAccount)
    }
}

@Composable
private fun AccountSpot(
    photoUrl: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = "Account"
                }.clickable(onClick = onClick),
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MetricLine(
    label: String,
    value: String,
) {
    Text(text = "$label: $value", color = Muted, fontSize = 14.sp)
}

@Composable
private fun CalendarBlock(
    days: List<ShellDay>,
    eventDates: Set<LocalDate>,
    onPlus: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = "Event calendar", color = Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onPlus) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(PlusYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add event", tint = Color(0xFF1A1A1A))
            }
        }
    }
    if (days.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { day ->
                DayCell(day = day, marked = day.date in eventDates)
            }
        }
    }
}

@Composable
private fun DayCell(
    day: ShellDay,
    marked: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = day.label, color = Muted, fontSize = 11.sp)
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (day.selected) Color.White else DayFill),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = day.dayOfMonth, color = if (day.selected) Color(0xFF161616) else Ink, fontSize = 13.sp)
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
private fun SavedPlaceCard(
    place: SavedLocation,
    onClick: () -> Unit,
) {
    val subtitle =
        listOf(place.city, place.state, place.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DayFill)
                .clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        if (place.name.isNotBlank()) {
            Text(text = place.name, color = Ink, fontSize = 16.sp)
        }
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, color = Muted, fontSize = 13.sp)
        }
    }
}

private val ScreenHorizontal = 20.dp
private val GradientTop = Color(0xFF3A342C)
private val GradientBottom = Color(0xFF14161C)
private val PlusYellow = Color(0xFFF5C400)
private val Ink = Color.White
private val Muted = Color.White.copy(alpha = 0.72f)
private val DayFill = Color(0x66101418)
