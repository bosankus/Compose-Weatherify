package bose.ankush.home.presentation.wander

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
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
import bose.ankush.home.presentation.places.WanderPlacesEffect
import bose.ankush.home.presentation.places.WanderPlacesIntent
import bose.ankush.home.presentation.places.WanderPlacesViewModel
import bose.ankush.home.presentation.shell.ShellCreateDialog
import bose.ankush.home.presentation.shell.ShellIntent
import bose.ankush.home.presentation.shell.ShellSectionKind
import bose.ankush.home.presentation.shell.ShellViewModel
import bose.ankush.home.presentation.shell.canSubmitEvent
import bose.ankush.home.presentation.shell.retryContentDescription
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android Wander home. Home is this shell, bound to the forecast already loaded
 * by [HomeViewModel]. Under the header a [HorizontalPager] holds two pages: the weather
 * column (alerts, air quality, hourly list) and the saved places list. The Map tab and the
 * saved place card both open the places page. Hub pushes Settings so back returns here.
 *
 * One [WanderTabBar] stays composed across both pages so the liquid pill springs can run
 * when the selected tab changes; swiping the pager moves the pill too.
 */
@Composable
internal fun WanderHomeScreen(
    links: WanderHomeLinks,
    shell: WanderShell,
    sections: WanderHomeSections = WanderHomeSections(),
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val places = sections.places
    val pager = rememberPagerState(pageCount = { WANDER_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var openAlert by remember { mutableStateOf<WeatherForecast.Alert?>(null) }
    val selectedTab = if (pager.currentPage == PLACES_PAGE) WanderTab.MAP else WanderTab.HOME
    val showPage = { page: Int ->
        scope.launch { pager.animateScrollToPage(page) }
        Unit
    }
    val onTab = { tab: WanderTab ->
        openAlert = null
        when (tab) {
            WanderTab.HOME -> showPage(WEATHER_PAGE)
            WanderTab.MAP -> showPage(PLACES_PAGE)
            WanderTab.HUB -> links.onOpenHub()
        }
    }
    // Back on the places page returns to the weather page; open sheets handle back first.
    BackHandler(enabled = pager.currentPage == PLACES_PAGE) { showPage(WEATHER_PAGE) }
    LaunchedEffect(places.effects) {
        places.effects.collect { effect ->
            when (effect) {
                WanderPlacesEffect.ShowWeather -> pager.animateScrollToPage(WEATHER_PAGE)
                WanderPlacesEffect.ShowPlaces -> pager.scrollToPage(PLACES_PAGE)
            }
        }
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage == PLACES_PAGE) places.onIntent(WanderPlacesIntent.Load)
    }

    val condition = shell.content.condition
    val photoUrl = shell.photo?.imageUrl
    val sample = rememberWanderImageSample(condition, photoUrl)
    val contentColor = contentColorForLuminance(sample.headerLuminance)

    Box(modifier = modifier.fillMaxSize()) {
        WanderHomePage(
            model =
                WanderHomePageModel(
                    shell = shell,
                    sections = sections,
                    contentColor = contentColor,
                    chipColors = rememberWanderChipColors(sample.averageColor),
                    cornerGlow = wanderCornerGlowColor(sample.averageColor, sample.headerLuminance),
                    openAlert = openAlert,
                    pager = pager,
                ),
            actions =
                WanderHomePageActions(
                    onOpenCalendar = onOpenCalendar,
                    onOpenAlert = { openAlert = it },
                    onDismissAlert = { openAlert = null },
                    onOpenPlaces = { showPage(PLACES_PAGE) },
                ),
            modifier = Modifier.fillMaxSize(),
        )
        WanderTabBar(
            selected = selectedTab,
            onSelected = onTab,
            inactiveTint = contentColor,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = TabBarBottomGap),
        )
    }
}

private data class WanderHomePageModel(
    val shell: WanderShell,
    val sections: WanderHomeSections,
    val contentColor: Color,
    val chipColors: WanderChipColors,
    val cornerGlow: Color,
    val openAlert: WeatherForecast.Alert?,
    val pager: PagerState,
)

private data class WanderHomePageActions(
    val onOpenCalendar: () -> Unit,
    val onOpenAlert: (WeatherForecast.Alert) -> Unit,
    val onDismissAlert: () -> Unit,
    val onOpenPlaces: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WanderHomePage(
    model: WanderHomePageModel,
    actions: WanderHomePageActions,
    modifier: Modifier = Modifier,
) {
    val shell = model.shell
    val chrome = model.sections.chrome
    val places = model.sections.places
    val pager = model.pager
    val content = shell.content
    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = content.condition, photoUrl = shell.photo?.imageUrl)
        WanderCornerGlow(color = model.cornerGlow)
        WanderNotificationPrompt(chrome)
        PullToRefreshBox(
            isRefreshing = chrome.refreshing,
            onRefresh = {
                chrome.onRefresh()
                if (pager.currentPage == PLACES_PAGE) places.onIntent(WanderPlacesIntent.Load)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val viewportHeight = maxHeight
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 40.dp),
                    ) {
                        WanderHeaderBlock(model = model, modifier = Modifier.padding(horizontal = 20.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalPager(
                            state = pager,
                            modifier = Modifier.fillMaxWidth(),
                            beyondViewportPageCount = 1,
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            val pageModifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            when (page) {
                                PLACES_PAGE ->
                                    WanderPlacesPage(
                                        places = places,
                                        contentColor = model.contentColor,
                                        minHeight = viewportHeight,
                                        modifier = pageModifier,
                                    )
                                else -> WanderWeatherPage(model = model, actions = actions, modifier = pageModifier)
                            }
                        }
                    }
                    WanderAlertPanel(alert = model.openAlert, onDismiss = actions.onDismissAlert)
                    WanderRefreshChip(
                        visible = chrome.backgroundRefreshing && !chrome.refreshing,
                        colors = model.chipColors,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                    )
                    WanderAddPlaceButton(
                        visible = pager.currentPage == PLACES_PAGE && places.state.isPremium,
                        onClick = { places.onIntent(WanderPlacesIntent.OpenSearch) },
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 16.dp),
                    )
                    WanderPlaceSearchSheet(places = places)
                }
                Spacer(modifier = Modifier.height(TabBarReserveHeight))
            }
        }
        WanderOfflineToast(chrome.offlineMessage)
    }
}

/** Observed time, location chip, and header. Stays above the pager on both pages. */
@Composable
private fun WanderHeaderBlock(
    model: WanderHomePageModel,
    modifier: Modifier = Modifier,
) {
    val chrome = model.sections.chrome
    val content = model.shell.content
    val contentColor = model.contentColor
    Column(modifier = modifier.fillMaxWidth()) {
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
            WanderAnimatedValue(
                text = observedLabel(observed, wanderForecastZone(chrome.timezoneOffset)),
                color = contentColor.copy(alpha = OBSERVED_ALPHA),
                fontSize = 13.sp,
                style = TextStyle(shadow = HeaderTextShadow),
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
            nearby = model.sections.nearby,
        )
    }
}

/** First pager page: every home section below the header, unchanged. */
@Composable
private fun WanderWeatherPage(
    model: WanderHomePageModel,
    actions: WanderHomePageActions,
    modifier: Modifier = Modifier,
) {
    val shell = model.shell
    val nearby = model.sections.nearby
    val forecast = model.sections.forecast
    val chrome = model.sections.chrome
    val contentColor = model.contentColor
    val content = shell.content
    val photo = shell.photo
    val photoUrl = photo?.imageUrl
    Column(modifier = modifier) {
        WanderDetailsGrid(
            content = content,
            current = chrome.current,
            contentColor = contentColor,
            extras = forecast.extras,
        )
        chrome.current?.let { current ->
            Spacer(modifier = Modifier.height(16.dp))
            WanderCurrentReport(
                current = current,
                timezoneOffset = chrome.timezoneOffset,
                contentColor = contentColor,
            )
        }
        forecast.extras.temperatureTrend?.let { trend ->
            Spacer(modifier = Modifier.height(12.dp))
            WanderTemperatureTrend(trend = trend, contentColor = contentColor)
        }
        if (content.days.isNotEmpty() || chrome.forecastFailed) {
            Spacer(modifier = Modifier.height(20.dp))
            WanderCalendarStrip(
                model =
                    WanderCalendarStripModel(
                        days = content.days,
                        eventDates = chrome.eventDates,
                        showWeekShimmer = false,
                    ),
                onOpen = actions.onOpenCalendar,
                contentColor = contentColor,
                onRetryCalendar = if (chrome.forecastFailed) chrome.onRetryForecast else null,
            )
        }
        shell.statusMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = contentColor.copy(alpha = 0.84f),
                fontSize = 13.sp,
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
            onOpenAlert = actions.onOpenAlert,
        )
        shell.leaveBy?.let { leaveBy ->
            Spacer(modifier = Modifier.height(16.dp))
            WanderLeaveByRow(leaveBy = leaveBy, contentColor = contentColor)
        }
        if (content.showSmallCards) {
            Spacer(modifier = Modifier.height(16.dp))
            WanderSmallCards(contentColor = contentColor)
        }
        val showNearby =
            nearby.events.isNotEmpty() ||
                nearby.savedPlace?.name?.isNotBlank() == true
        if (showNearby) {
            Spacer(modifier = Modifier.height(16.dp))
            WanderNearbyBlocks(
                nearby = nearby.copy(eventsLoading = false, eventsFailed = nearby.eventsFailed),
                contentColor = contentColor,
                onOpenSaved = actions.onOpenPlaces,
            )
        }
        if (photo != null && photoUrl != null && photo.downloadLocation.isNotBlank()) {
            TrackShownWanderPhoto(photo)
            Spacer(modifier = Modifier.height(8.dp))
            UnsplashCredit(photo = photo, contentColor = contentColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
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
    val placesViewModel = koinViewModel<WanderPlacesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shellState by shellViewModel.state.collectAsStateWithLifecycle()
    val placesState by placesViewModel.state.collectAsStateWithLifecycle()
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
    val liveContent = state.weatherData?.takeIf { current != null }?.toWanderContent(place)
    val content =
        liveContent
            ?: placeholderWanderContent(
                place = place.takeIf { it != CURRENT_LOCATION } ?: WANDER_PLACEHOLDER,
                condition = WanderCondition.CLOUDS,
            )
    val photo = rememberWanderConditionPhoto(weatherMain?.let(::unsplashQuery), content.condition)
    val leaveBy = leaveByRow(state, viewModel)
    val shell =
        WanderShell(
            content = content,
            photo = photo,
            leaveBy = leaveBy,
            statusMessage =
                (state.error ?: state.offlineMessage)?.takeIf {
                    liveContent == null && it.isNotBlank()
                },
        )
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
            sections =
                WanderHomeSections(
                    nearby =
                        shellState.toWanderNearby(
                            onOpenAccount = links.onOpenHub,
                            onRetryEvents = shellViewModel::retryEvents,
                        ),
                    forecast =
                        WanderForecastDetails(
                            alerts = state.weatherData?.alerts.orEmpty(),
                            airQuality = state.airQualityData,
                            hourly = state.weatherData?.hourly.orEmpty(),
                            extras = state.weatherData?.toWanderForecastExtras() ?: WanderForecastExtras(),
                        ),
                    chrome =
                        rememberWanderChrome(
                            state = state,
                            shellState = shellState,
                            forecastVisible = liveContent != null,
                            viewModel = viewModel,
                        ),
                    places =
                        WanderPlacesBinding(
                            state = placesState,
                            effects = placesViewModel.effect,
                            onIntent = placesViewModel::processIntent,
                            onUpgrade = links.onOpenHub,
                        ),
                ),
            onOpenCalendar = { shellViewModel.onIntent(ShellIntent.OpenCreate) },
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
private const val WANDER_PAGE_COUNT = 2
private const val WEATHER_PAGE = 0
private const val PLACES_PAGE = 1
internal val TabBarHeight = 64.dp
internal val TabBarBottomGap = 12.dp
internal val TabBarReserveHeight = TabBarHeight + TabBarBottomGap
internal val HeaderTextShadow =
    Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 8f)
