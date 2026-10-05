package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.HomeIntent
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.shell.ShellCreateDialog
import bose.ankush.home.presentation.shell.ShellIntent
import bose.ankush.home.presentation.shell.ShellSectionKind
import bose.ankush.home.presentation.shell.ShellViewModel
import bose.ankush.home.presentation.shell.canSubmitEvent
import bose.ankush.home.presentation.shell.retryContentDescription
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android Wander home. Home is this shell, bound to the forecast already loaded
 * by [HomeViewModel]. Alerts, air quality, and the hourly list sit on this column.
 * Map is saved places. Hub pushes Settings so back returns here. There is no weather tab.
 */
@Composable
fun WanderHomeScreen(
    links: WanderHomeLinks,
    shell: WanderShell,
    modifier: Modifier = Modifier,
    nearby: WanderNearby = WanderNearby(),
    onOpenCalendar: () -> Unit = {},
    forecast: WanderForecastDetails = WanderForecastDetails(),
    chrome: WanderHomeChrome = WanderHomeChrome(),
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
                    nearby = nearby,
                    onOpenCalendar = onOpenCalendar,
                    chrome = chrome,
                    modifier = modifier,
                )
            is WanderShell.Ready ->
                WanderHomePage(
                    ready = shell,
                    selectedTab = selectedTab,
                    onTab = onTab,
                    nearby = nearby,
                    onOpenCalendar = onOpenCalendar,
                    forecast = forecast,
                    chrome = chrome,
                    modifier = modifier,
                )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    WanderTab.MAP -> links.places()
                    WanderTab.HOME, WanderTab.HUB -> Unit
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
    nearby: WanderNearby,
    onOpenCalendar: () -> Unit,
    forecast: WanderForecastDetails,
    chrome: WanderHomeChrome,
    modifier: Modifier = Modifier,
) {
    val content = ready.content
    val photo = ready.photo
    val photoUrl = photo?.imageUrl
    val contentColor = rememberWanderContentColor(content.condition, photoUrl)
    var openAlert by remember { mutableStateOf<WeatherForecast.Alert?>(null) }
    val onHomeTab = { tab: WanderTab ->
        openAlert = null
        onTab(tab)
    }
    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = content.condition, photoUrl = photoUrl)
        WanderNotificationPrompt(chrome)
        PullToRefreshBox(
            isRefreshing = chrome.refreshing,
            onRefresh = chrome.onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                    ) {
                        Spacer(modifier = Modifier.height(28.dp))
                        chrome.locationOverrideName?.let { name ->
                            WanderLocationChip(
                                label = name,
                                name = name,
                                onReset = chrome.onResetLocation,
                                contentColor = contentColor,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        chrome.current?.dt?.let { observed ->
                            Text(
                                text = observedLabel(observed, wanderForecastZone()),
                                color = contentColor.copy(alpha = OBSERVED_ALPHA),
                                fontSize = 13.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        WanderHeader(
                            temperature = content.temperature,
                            place = content.place,
                            conditionLine =
                                wanderHeaderLine(
                                    summary = chrome.todaySummary,
                                    description =
                                        chrome.current
                                            ?.weather
                                            ?.firstOrNull()
                                            ?.description,
                                    fallback = content.condition.line,
                                ),
                            contentColor = contentColor,
                            nearby = nearby,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        WanderDetailsGrid(
                            content = content,
                            current = chrome.current,
                            contentColor = contentColor,
                        )
                        chrome.current?.let { current ->
                            Spacer(modifier = Modifier.height(16.dp))
                            WanderCurrentReport(
                                current = current,
                                contentColor = contentColor,
                            )
                        }
                        if (content.days.isNotEmpty() || chrome.forecastFailed) {
                            Spacer(modifier = Modifier.height(20.dp))
                            WanderCalendarStrip(
                                days = content.days,
                                onOpen = onOpenCalendar,
                                contentColor = contentColor,
                                eventDates = chrome.eventDates,
                                showWeekShimmer = content.days.isEmpty() && chrome.forecastLoading,
                                onRetryCalendar = if (chrome.forecastFailed) chrome.onRetryForecast else null,
                            )
                        }
                        if (chrome.forecastFailed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            WanderActionLabel(
                                label = retryContentDescription(ShellSectionKind.Forecast),
                                onClick = chrome.onRetryForecast,
                                contentColor = contentColor,
                            )
                        }
                        WanderForecastDetails(
                            details = forecast,
                            contentColor = contentColor,
                            onOpenAlert = { openAlert = it },
                        )
                        ready.leaveBy?.let { leaveBy ->
                            Spacer(modifier = Modifier.height(16.dp))
                            WanderLeaveByRow(leaveBy = leaveBy, contentColor = contentColor)
                        }
                        if (content.showSmallCards) {
                            Spacer(modifier = Modifier.height(16.dp))
                            WanderSmallCards(contentColor = contentColor)
                        }
                        if (nearby.eventsLoading || nearby.eventsFailed || nearby.events.isNotEmpty() ||
                            nearby.savedPlace != null
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            WanderNearbyBlocks(
                                nearby = nearby,
                                contentColor = contentColor,
                                onOpenSaved = { onHomeTab(WanderTab.MAP) },
                            )
                        }
                        if (photo != null && photoUrl != null) {
                            TrackShownWanderPhoto(photo)
                            Spacer(modifier = Modifier.height(8.dp))
                            UnsplashCredit(photo = photo, contentColor = contentColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    WanderAlertPanel(alert = openAlert, onDismiss = { openAlert = null })
                }
                WanderTabBar(
                    selected = selectedTab,
                    onSelected = onHomeTab,
                    inactiveTint = contentColor,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        WanderOfflineToast(chrome.offlineMessage)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WanderWaitingPage(
    waiting: WanderShell.Waiting,
    selectedTab: WanderTab,
    onTab: (WanderTab) -> Unit,
    nearby: WanderNearby,
    onOpenCalendar: () -> Unit,
    chrome: WanderHomeChrome,
    modifier: Modifier = Modifier,
) {
    val contentColor = rememberWanderContentColor(WanderCondition.CLOUDS, photoUrl = null)
    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = WanderCondition.CLOUDS)
        WanderNotificationPrompt(chrome)
        PullToRefreshBox(
            isRefreshing = chrome.refreshing,
            onRefresh = chrome.onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(28.dp))
                    if (nearby.showAccount) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            WanderAccountMark(photoUrl = nearby.photoUrl, onOpen = nearby.onOpenAccount)
                        }
                    }
                    chrome.locationOverrideName?.let { name ->
                        WanderLocationChip(
                            label = name,
                            name = name,
                            onReset = chrome.onResetLocation,
                            contentColor = contentColor,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (chrome.forecastLoading || chrome.forecastFailed) {
                        WeekHold(showShimmer = chrome.forecastLoading || waiting.loading)
                        Spacer(modifier = Modifier.height(16.dp))
                        WanderCalendarStrip(
                            days = emptyList(),
                            onOpen = onOpenCalendar,
                            contentColor = contentColor,
                            showWeekShimmer = true,
                            onRetryCalendar = if (chrome.forecastFailed) chrome.onRetryForecast else null,
                        )
                    }
                    if (!waiting.loading && !waiting.statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = waiting.statusMessage, color = contentColor, fontSize = 18.sp)
                    }
                    if (!waiting.loading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        WanderWaitingActions(chrome = chrome, contentColor = contentColor)
                    }
                    waiting.leaveBy?.let { leaveBy ->
                        Spacer(modifier = Modifier.height(16.dp))
                        WanderLeaveByRow(leaveBy = leaveBy, contentColor = contentColor)
                    }
                    if (nearby.eventsLoading || nearby.eventsFailed || nearby.events.isNotEmpty() ||
                        nearby.savedPlace != null
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        WanderNearbyBlocks(
                            nearby = nearby,
                            contentColor = contentColor,
                            onOpenSaved = { onTab(WanderTab.MAP) },
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                WanderTabBar(
                    selected = selectedTab,
                    onSelected = onTab,
                    inactiveTint = contentColor,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WeekHold(showShimmer: Boolean) {
    if (!showShimmer) return
    val brush = rememberWanderShimmerBrush()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(brush),
    )
}

@Composable
private fun WanderHeader(
    temperature: String,
    place: String,
    conditionLine: String,
    contentColor: Color,
    nearby: WanderNearby,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        WanderTemperatureHeader(
            temperature = temperature,
            place = place,
            conditionLine = conditionLine,
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        )
        if (nearby.showAccount) {
            Spacer(modifier = Modifier.width(12.dp))
            WanderAccountMark(photoUrl = nearby.photoUrl, onOpen = nearby.onOpenAccount)
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
    val shellViewModel = koinViewModel<ShellViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shellState by shellViewModel.state.collectAsStateWithLifecycle()
    WanderCollectHomeEffects(viewModel)
    LaunchedEffect(Unit) {
        shellViewModel.refreshAccount()
    }
    LaunchedEffect(state.userLocation) {
        val location = state.userLocation ?: return@LaunchedEffect
        shellViewModel.onIntent(ShellIntent.LocationUpdated(location.first, location.second))
    }
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
    val placeName = state.activeLocationName?.takeIf { it.isNotBlank() } ?: place.takeIf { it != CURRENT_LOCATION }
    LaunchedEffect(placeName) {
        if (!placeName.isNullOrBlank()) {
            shellViewModel.onIntent(ShellIntent.PlaceNameUpdated(placeName))
        }
    }
    Box(modifier = modifier) {
        WanderHomeScreen(
            links = links,
            shell = shell,
            onOpenCalendar = { shellViewModel.onIntent(ShellIntent.OpenCreate) },
            forecast =
                WanderForecastDetails(
                    alerts = state.weatherData?.alerts.orEmpty(),
                    airQuality = state.airQualityData,
                    hourly = state.weatherData?.hourly.orEmpty(),
                ),
            chrome =
                rememberWanderChrome(
                    state = state,
                    shellState = shellState,
                    forecastVisible = content != null,
                    viewModel = viewModel,
                ),
            nearby =
                shellState.toWanderNearby(
                    onOpenAccount = links.onOpenHub,
                    onRetryEvents = shellViewModel::retryEvents,
                ),
        )
        ShellCreateDialog(
            state = shellState,
            canSave = canSubmitEvent(shellState),
            onIntent = shellViewModel::onIntent,
            onSave = shellViewModel::submitCreate,
        )
    }
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
private const val OBSERVED_ALPHA = 0.72f
