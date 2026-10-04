package bose.ankush.home.presentation.wander

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.presentation.HomeIntent
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.state.ShowLoading
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android Wander home. Home is this shell, bound to the forecast already loaded
 * by [HomeViewModel]. Weather is that same forecast. Map is saved places.
 * Hub pushes Settings so back returns here. There is no travel tab.
 */
@Composable
fun WanderHomeScreen(
    links: WanderHomeLinks,
    shell: WanderShell,
    modifier: Modifier = Modifier,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(WanderTab.HOME.name) }
    val selectedTab = WanderTab.entries.firstOrNull { it.name == selectedTabName } ?: WanderTab.HOME

    val onTab = { tab: WanderTab ->
        when (tab) {
            WanderTab.HUB -> links.onOpenHub()
            else -> selectedTabName = tab.name
        }
    }

    if (selectedTab == WanderTab.HOME) {
        when (shell) {
            is WanderShell.Waiting ->
                WanderWaitingPage(
                    waiting = shell,
                    selectedTab = selectedTab,
                    onTab = onTab,
                    modifier = modifier,
                )
            is WanderShell.Ready ->
                WanderHomePage(
                    ready = shell,
                    selectedTab = selectedTab,
                    onTab = onTab,
                    modifier = modifier,
                )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    WanderTab.WEATHER -> links.weather()
                    WanderTab.MAP -> links.places()
                    else -> Unit
                }
            }
            WanderTabBar(
                selected = selectedTab,
                onSelected = onTab,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun WanderHomePage(
    ready: WanderShell.Ready,
    selectedTab: WanderTab,
    onTab: (WanderTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = ready.content
    val photo = ready.photo
    val photoUrl = photo?.imageUrl
    val contentColor = rememberWanderContentColor(content.condition, photoUrl)
    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = content.condition, photoUrl = photoUrl)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            WanderTemperatureHeader(
                temperature = content.temperature,
                place = content.place,
                conditionLine = content.condition.line,
                contentColor = contentColor,
            )
            Spacer(modifier = Modifier.height(28.dp))
            WanderMetricChips(
                feel = content.feel,
                wind = content.wind,
                uv = content.uv,
                contentColor = contentColor,
            )
            if (content.days.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                WanderCalendarStrip(days = content.days, contentColor = contentColor)
            }
            ready.leaveBy?.let { leaveBy ->
                Spacer(modifier = Modifier.height(16.dp))
                WanderLeaveByRow(leaveBy = leaveBy, contentColor = contentColor)
            }
            if (content.showSmallCards) {
                Spacer(modifier = Modifier.height(16.dp))
                WanderSmallCards(contentColor = contentColor)
            }
            if (photo != null && photoUrl != null) {
                TrackShownWanderPhoto(photo)
                Spacer(modifier = Modifier.height(8.dp))
                UnsplashCredit(photo = photo, contentColor = contentColor)
            }
            Spacer(modifier = Modifier.weight(1f))
            WanderTabBar(selected = selectedTab, onSelected = onTab, inactiveTint = contentColor)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WanderWaitingPage(
    waiting: WanderShell.Waiting,
    selectedTab: WanderTab,
    onTab: (WanderTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = rememberWanderContentColor(WanderCondition.CLOUDS, photoUrl = null)
    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = WanderCondition.CLOUDS)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            if (waiting.loading) {
                ShowLoading(modifier = Modifier.fillMaxWidth())
            } else if (!waiting.statusMessage.isNullOrBlank()) {
                Text(text = waiting.statusMessage, color = contentColor, fontSize = 18.sp)
            }
            waiting.leaveBy?.let { leaveBy ->
                Spacer(modifier = Modifier.height(16.dp))
                WanderLeaveByRow(leaveBy = leaveBy, contentColor = contentColor)
            }
            Spacer(modifier = Modifier.weight(1f))
            WanderTabBar(selected = selectedTab, onSelected = onTab, inactiveTint = contentColor)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun UnsplashCredit(
    photo: WanderFogPhoto,
    contentColor: Color,
) {
    val linkStyle =
        TextLinkStyles(
            SpanStyle(
                color = contentColor,
                textDecoration = TextDecoration.Underline,
            ),
        )
    Text(
        text =
            buildAnnotatedString {
                append("Photo by ")
                withLink(LinkAnnotation.Url(photo.profileUrl, linkStyle)) {
                    append(photo.photographer)
                }
                append(" on ")
                withLink(LinkAnnotation.Url(UNSPLASH_HOME_URL, linkStyle)) {
                    append("Unsplash")
                }
            },
        color = contentColor,
        fontSize = 13.sp,
    )
}

/**
 * App entry. Uses the forecast [HomeViewModel] already loads. The preview does not
 * call this, so it never touches Koin or the network.
 */
@Composable
fun WanderHomeRoute(
    links: WanderHomeLinks,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.weatherData?.current
    val place = rememberForecastPlace(state.userLocation)
    val weatherMain =
        current
            ?.weather
            ?.firstOrNull()
            ?.main
            ?.takeIf { it.isNotBlank() }
    val photo = rememberWanderConditionPhoto(weatherMain?.let(::unsplashQuery))
    val content = state.weatherData?.takeIf { current != null }?.toWanderContent(place)
    val leaveBy = leaveByRow(state, viewModel)
    val shell =
        if (content == null) {
            WanderShell.Waiting(
                loading = state.isLoading,
                statusMessage = state.error ?: state.offlineMessage,
                leaveBy = leaveBy,
            )
        } else {
            WanderShell.Ready(content = content, photo = photo, leaveBy = leaveBy)
        }
    WanderHomeScreen(
        links = links,
        shell = shell,
        modifier = modifier,
    )
}

/** Same resolver as the forecast header: reverse-geocode the loaded coordinates. */
@Composable
private fun rememberForecastPlace(userLocation: Pair<Double, Double>?): String {
    val geocoder = koinInject<HomeGeocoder>()
    var place by remember(userLocation) { mutableStateOf(CURRENT_LOCATION) }
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            place = geocoder.reverseGeocode(userLocation.first, userLocation.second) ?: CURRENT_LOCATION
        }
    }
    return place
}

private fun leaveByRow(
    state: HomeState,
    viewModel: HomeViewModel,
): WanderLeaveBy? {
    if (!state.showLeaveByCard) return null
    return WanderLeaveBy(
        hasJoined = state.hasJoinedLeaveByList,
        hasNotedMisleading = state.hasNotedLeaveByMisleading,
        onJoin = { viewModel.processIntent(HomeIntent.JoinLeaveByList) },
        onDismiss = { viewModel.processIntent(HomeIntent.DismissLeaveByCard) },
        onMisleading = { viewModel.processIntent(HomeIntent.NoteLeaveByMisleading) },
    )
}

private const val CURRENT_LOCATION = "Current Location"
