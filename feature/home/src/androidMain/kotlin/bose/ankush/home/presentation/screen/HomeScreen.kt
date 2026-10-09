package bose.ankush.home.presentation.screen

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.account.AccountAvatarIntent
import bose.ankush.home.presentation.account.AccountAvatarViewModel
import bose.ankush.home.presentation.ai.AiSummaryIntent
import bose.ankush.home.presentation.ai.AiSummaryViewModel
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyViewModel
import bose.ankush.home.presentation.places.SavedPlacesEffect
import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.SavedPlacesViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android home screen. Home is this shell, bound to the forecast already loaded
 * by [HomeViewModel]. Under the header a [HorizontalPager] holds two pages: the weather
 * column (alerts, air quality, hourly list) and the saved places list. The Map tab and the
 * saved place card both open the places page. Hub pushes Settings so back returns here.
 *
 * One [HomeTabBar] stays composed across both pages so the liquid pill springs can run
 * when the selected tab changes; swiping the pager moves the pill too.
 */
@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    links: HomeScreenLinks,
    shell: HomeScreenShell,
    sections: HomeScreenSections,
    onOpenCalendar: () -> Unit = {},
) {
    val places = sections.places
    val pager = rememberPagerState(pageCount = { HOME_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var openAlert by remember { mutableStateOf<WeatherForecast.Alert?>(null) }
    var showAiSummary by remember { mutableStateOf(false) }
    val aiSummary = sections.aiSummary
    val closeAiSummary = {
        showAiSummary = false
        aiSummary.onDismiss()
    }
    val selectedTab = if (pager.currentPage == PLACES_PAGE) HomeTab.MAP else HomeTab.HOME
    val showPage = { page: Int ->
        scope.launch { pager.animateScrollToPage(page) }
        Unit
    }
    val onTab = { tab: HomeTab ->
        openAlert = null
        closeAiSummary()
        when (tab) {
            HomeTab.HOME -> showPage(WEATHER_PAGE)
            HomeTab.MAP -> showPage(PLACES_PAGE)
            HomeTab.HUB -> links.onOpenHub()
        }
    }
    // Back on the places page returns to the weather page; open sheets handle back first.
    BackHandler(enabled = pager.currentPage == PLACES_PAGE) { showPage(WEATHER_PAGE) }
    LaunchedEffect(places.effects) {
        places.effects.collect { effect ->
            when (effect) {
                SavedPlacesEffect.ShowWeather -> pager.animateScrollToPage(WEATHER_PAGE)
                SavedPlacesEffect.ShowPlaces -> pager.scrollToPage(PLACES_PAGE)
            }
        }
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage == PLACES_PAGE) places.onIntent(SavedPlacesIntent.Load)
    }

    val condition = shell.content.condition
    val photoUrl = shell.photo?.imageUrl
    val sample = rememberBackgroundImageSample(condition, photoUrl)
    val contentColor = contentColorForLuminance(sample.headerLuminance)
    // One instance until a callback's inputs change, so the pager pages can skip.
    val actions =
        remember(onOpenCalendar, aiSummary, closeAiSummary, showPage) {
            HomePagerActions(
                onOpenCalendar = onOpenCalendar,
                onOpenAlert = { openAlert = it },
                onDismissAlert = { openAlert = null },
                onOpenAiSummary = {
                    showAiSummary = true
                    aiSummary.onOpen()
                },
                onDismissAiSummary = closeAiSummary,
                onOpenPlaces = { showPage(PLACES_PAGE) },
            )
        }

    Box(modifier = modifier.fillMaxSize()) {
        HomePager(
            model =
                HomePagerModel(
                    shell = shell,
                    sections = sections,
                    contentColor = contentColor,
                    chipColors = rememberMetricChipColors(sample.averageColor),
                    cornerGlow = cornerGlowColor(sample.averageColor, sample.headerLuminance),
                    openAlert = openAlert,
                    aiSummaryVisible = showAiSummary,
                    pager = pager,
                ),
            actions = actions,
            modifier = modifier.fillMaxSize(),
        )
        HomeTabBar(
            selected = selectedTab,
            onSelected = onTab,
            inactiveTint = contentColor,
            modifier =
                modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = TabBarBottomGap),
        )
    }
}

private data class HomePagerModel(
    val shell: HomeScreenShell,
    val sections: HomeScreenSections,
    val contentColor: Color,
    val chipColors: MetricChipColors,
    val cornerGlow: Color,
    val openAlert: WeatherForecast.Alert?,
    val aiSummaryVisible: Boolean,
    val pager: PagerState,
)

private data class HomePagerActions(
    val onOpenCalendar: () -> Unit,
    val onOpenAlert: (WeatherForecast.Alert) -> Unit,
    val onDismissAlert: () -> Unit,
    val onOpenAiSummary: () -> Unit,
    val onDismissAiSummary: () -> Unit,
    val onOpenPlaces: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePager(
    model: HomePagerModel,
    actions: HomePagerActions,
    modifier: Modifier = Modifier,
) {
    val shell = model.shell
    val chrome = model.sections.chrome
    val places = model.sections.places
    val pager = model.pager
    val content = shell.content
    Box(modifier = modifier.fillMaxSize()) {
        SkyConditionBackground(condition = content.condition, photoUrl = shell.photo?.imageUrl)
        CornerGlow(color = model.cornerGlow)
        NotificationPrompt(chrome)
        PullToRefreshBox(
            isRefreshing = chrome.refreshing,
            onRefresh = {
                chrome.onRefresh()
                if (pager.currentPage == PLACES_PAGE) places.onIntent(SavedPlacesIntent.Load)
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
                        HomeHeaderBlock(
                            content = content,
                            chrome = chrome,
                            nearby = model.sections.nearby,
                            contentColor = model.contentColor,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
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
                                    SavedPlacesPage(
                                        places = places,
                                        contentColor = model.contentColor,
                                        minHeight = viewportHeight,
                                        modifier = pageModifier,
                                    )

                                else ->
                                    WeatherPage(
                                        shell = shell,
                                        chrome = chrome,
                                        forecast = model.sections.forecast,
                                        nearby = model.sections.nearby,
                                        aiSummaryAvailable = model.sections.aiSummary.isAvailable,
                                        contentColor = model.contentColor,
                                        actions = actions,
                                        modifier = pageModifier,
                                    )
                            }
                        }
                    }
                    WeatherAlertPanel(alert = model.openAlert, onDismiss = actions.onDismissAlert)
                    RefreshChip(
                        visible = chrome.backgroundRefreshing && !chrome.refreshing,
                        colors = model.chipColors,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                    )
                    AddPlaceButton(
                        visible =
                            AddPlaceAction.showAddButton(
                                onPlacesPage = pager.currentPage == PLACES_PAGE,
                                isPremium = places.state.isPremium,
                                sheetOpen = places.state.search.isOpen,
                            ),
                        onClick = { places.onIntent(SavedPlacesIntent.OpenSearch) },
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 16.dp),
                    )
                    PlaceSearchSheet(places = places)
                    CreateEventSheet(event = model.sections.events)
                    AiSummarySheet(
                        visible = model.aiSummaryVisible,
                        binding = model.sections.aiSummary,
                        onDismiss = actions.onDismissAiSummary,
                    )
                }
                Spacer(modifier = Modifier.height(TabBarReserveHeight))
            }
        }
        OfflineToast(chrome.offlineMessage)
    }
}

/** Observed time, location chip, and header. Stays above the pager on both pages. */
@Composable
private fun HomeHeaderBlock(
    content: HomeWeatherContent,
    chrome: HomeScreenChrome,
    nearby: NearbyContent,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(28.dp))
        chrome.locationOverrideName?.let { name ->
            LocationOverrideChip(
                label = name,
                name = name,
                onReset = chrome.onResetLocation,
                contentColor = contentColor,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        chrome.current?.dt?.let { observed ->
            AnimatedValueText(
                text = observedLabel(observed, forecastZone(chrome.timezoneOffset)),
                color = contentColor.copy(alpha = OBSERVED_ALPHA),
                fontSize = 13.sp,
                style = TextStyle(shadow = HeaderTextShadow),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        HomeHeader(
            temperature = content.temperature,
            place = content.place,
            conditionLine =
                headerSummaryLine(
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
    }
}

/** First pager page: every home section below the header, unchanged. */
@Composable
private fun WeatherPage(
    shell: HomeScreenShell,
    chrome: HomeScreenChrome,
    forecast: ForecastDetails,
    nearby: NearbyContent,
    aiSummaryAvailable: Boolean,
    contentColor: Color,
    actions: HomePagerActions,
    modifier: Modifier = Modifier,
) {
    SideEffect { Log.d(PERF_TAG, "WeatherPage recomposed") } // TEMP(perf)
    val content = shell.content
    val photo = shell.photo
    val photoUrl = photo?.imageUrl
    Column(modifier = modifier) {
        WeatherDetailsGrid(
            content = content,
            current = chrome.current,
            contentColor = contentColor,
            extras = forecast.extras,
        )
        chrome.current?.let { current ->
            Spacer(modifier = Modifier.height(16.dp))
            CurrentWeatherReport(
                current = current,
                timezoneOffset = chrome.timezoneOffset,
                contentColor = contentColor,
            )
        }
        forecast.extras.temperatureTrend?.let { trend ->
            Spacer(modifier = Modifier.height(12.dp))
            TemperatureTrend(trend = trend, contentColor = contentColor)
        }
        if (content.days.isNotEmpty() || chrome.forecastFailed) {
            Spacer(modifier = Modifier.height(20.dp))
            WeekCalendarStrip(
                model =
                    WeekCalendarStripModel(
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
            ActionLabel(
                label = RetryLabels.FORECAST,
                onClick = chrome.onRetryForecast,
                contentColor = contentColor,
            )
        }
        ForecastDetails(
            details = forecast,
            contentColor = contentColor,
            onOpenAlert = actions.onOpenAlert,
        )
        if (aiSummaryAvailable) {
            Spacer(modifier = Modifier.height(16.dp))
            AiSummaryEntry(contentColor = contentColor, onClick = actions.onOpenAiSummary)
        }
        if (content.showSmallCards) {
            Spacer(modifier = Modifier.height(16.dp))
            PromoSmallCards(contentColor = contentColor)
        }
        val showNearby =
            nearby.events.isNotEmpty() ||
                nearby.savedPlace?.name?.isNotBlank() == true
        if (showNearby) {
            Spacer(modifier = Modifier.height(16.dp))
            val weatherPageNearby = remember(nearby) { nearby.copy(eventsLoading = false) }
            NearbyBlocks(
                nearby = weatherPageNearby,
                contentColor = contentColor,
                onOpenSaved = actions.onOpenPlaces,
            )
        }
        if (photo != null && photoUrl != null && photo.downloadLocation.isNotBlank()) {
            TrackShownBackgroundPhoto(photo)
            Spacer(modifier = Modifier.height(8.dp))
            AppBgPhotoCredit(photo = photo, contentColor = contentColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun HomeHeader(
    temperature: String,
    place: String,
    conditionLine: String,
    contentColor: Color,
    nearby: NearbyContent,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TemperatureHeader(
            temperature = temperature,
            place = place,
            conditionLine = conditionLine,
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        )
        if (nearby.showAccount) {
            Spacer(modifier = Modifier.width(12.dp))
            AccountMark(photoUrl = nearby.photoUrl, onOpen = nearby.onOpenAccount)
        }
    }
}

@Composable
private fun AppBgPhotoCredit(
    photo: BackgroundPhoto,
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
fun HomeScreenRoute(
    links: HomeScreenLinks,
    modifier: Modifier = Modifier,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val nearbyViewModel = koinViewModel<NearbyViewModel>()
    val avatarViewModel = koinViewModel<AccountAvatarViewModel>()
    val aiSummaryViewModel = koinViewModel<AiSummaryViewModel>()
    val placesViewModel = koinViewModel<SavedPlacesViewModel>()
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val nearbyState by nearbyViewModel.state.collectAsStateWithLifecycle()
    val avatarState by avatarViewModel.state.collectAsStateWithLifecycle()
    val placesState by placesViewModel.state.collectAsStateWithLifecycle()
    val aiSummaryState by aiSummaryViewModel.state.collectAsStateWithLifecycle()
    CollectHomeEffects(homeViewModel)
    LaunchedEffect(Unit) {
        avatarViewModel.onIntent(AccountAvatarIntent.Refresh)
    }
    LaunchedEffect(state.userLocation) {
        val location = state.userLocation ?: return@LaunchedEffect
        nearbyViewModel.onIntent(
            NearbyIntent.LocationChanged(
                GeoPoint(
                    location.first,
                    location.second,
                ),
            ),
        )
    }
    SideEffect { Log.d(PERF_TAG, "HomeScreenRoute recomposed") } // TEMP(perf)

    // The mappings below are pure functions of the forecast, so they are remembered per forecast
    // instead of re-run on every recomposition (places, nearby and AI state all recompose this body).
    val weather = state.weatherData
    val place = rememberForecastPlace(state.userLocation)
    val weatherMain =
        weather
            ?.current
            ?.weather
            ?.firstOrNull()
            ?.main
            ?.takeIf { it.isNotBlank() }
    val liveContent =
        remember(weather, place) {
            Log.d(PERF_TAG, "toHomeWeatherContent") // TEMP(perf)
            weather?.takeIf { it.current != null }?.toHomeWeatherContent(place)
        }
    val content =
        remember(liveContent, place) {
            liveContent
                ?: placeholderHomeWeatherContent(
                    place = place.takeIf { it != CURRENT_LOCATION } ?: VALUE_PLACEHOLDER,
                    condition = SkyCondition.CLOUDS,
                )
        }
    val extras =
        remember(weather) {
            Log.d(PERF_TAG, "toForecastExtras") // TEMP(perf)
            weather?.toForecastExtras() ?: ForecastExtras()
        }
    val photo = rememberBackgroundPhoto(weatherMain?.let(::unsplashQuery), content.condition)
    val statusMessage =
        (state.error ?: state.offlineMessage)?.takeIf { liveContent == null && it.isNotBlank() }
    val shell = remember(content, photo, statusMessage) { HomeScreenShell(content, photo, statusMessage) }
    val placeName = state.activeLocationName?.takeIf { it.isNotBlank() } ?: place.takeIf { it != CURRENT_LOCATION }
    LaunchedEffect(placeName) {
        if (!placeName.isNullOrBlank()) {
            nearbyViewModel.onIntent(NearbyIntent.PlaceNameChanged(placeName))
        }
    }
    val nearby =
        remember(nearbyState, avatarState.photoUrl, links, nearbyViewModel) {
            nearbyState.toNearbyContent(
                photoUrl = avatarState.photoUrl,
                onOpenAccount = links.onOpenHub,
                onRetryEvents = { nearbyViewModel.onIntent(NearbyIntent.RetryEvents) },
            )
        }
    val forecast =
        remember(weather, state.airQualityData, extras) {
            ForecastDetails(
                alerts = weather?.alerts.orEmpty(),
                airQuality = state.airQualityData,
                hourly = weather?.hourly.orEmpty(),
                extras = extras,
            )
        }
    val chrome =
        rememberHomeScreenChrome(
            state = state,
            eventDates = nearbyState.eventDates,
            forecastVisible = liveContent != null,
            viewModel = homeViewModel,
        )
    val places =
        remember(placesState, placesViewModel, links) {
            SavedPlacesBinding(
                state = placesState,
                effects = placesViewModel.effect,
                onIntent = placesViewModel::processIntent,
                onUpgrade = links.onOpenHub,
            )
        }
    val events =
        remember(nearbyState, nearbyViewModel) {
            CreateEventBinding(state = nearbyState, onIntent = nearbyViewModel::onIntent)
        }
    val aiSummary =
        remember(aiSummaryState, weather, placeName, aiSummaryViewModel) {
            AiSummaryBinding(
                state = aiSummaryState.summary,
                isAvailable = aiSummaryState.isAvailable && weather != null,
                onOpen = {
                    weather?.let { aiSummaryViewModel.onIntent(AiSummaryIntent.Open(it, placeName)) }
                },
                onRetry = { aiSummaryViewModel.onIntent(AiSummaryIntent.Retry) },
                onDismiss = { aiSummaryViewModel.onIntent(AiSummaryIntent.Dismiss) },
            )
        }
    val sections =
        remember(nearby, forecast, chrome, places, events, aiSummary) {
            HomeScreenSections(
                nearby = nearby,
                forecast = forecast,
                chrome = chrome,
                places = places,
                events = events,
                aiSummary = aiSummary,
            )
        }
    Box(modifier = modifier) {
        HomeScreen(
            links = links,
            shell = shell,
            sections = sections,
            onOpenCalendar = { nearbyViewModel.onIntent(NearbyIntent.OpenComposer) },
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

private const val CURRENT_LOCATION = "Current Location"
private const val PERF_TAG = "HomePerf" // TEMP(perf)
private const val OBSERVED_ALPHA = 0.72f
private const val HOME_PAGE_COUNT = 2
private const val WEATHER_PAGE = 0
private const val PLACES_PAGE = 1
internal val TabBarHeight = 64.dp
internal val TabBarBottomGap = 12.dp
internal val TabBarReserveHeight = TabBarHeight + TabBarBottomGap
internal val HeaderTextShadow =
    Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 8f)
