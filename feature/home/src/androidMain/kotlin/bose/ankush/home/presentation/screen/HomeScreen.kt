package bose.ankush.home.presentation.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
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
import bose.ankush.home.HomeFirstContent
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.account.AccountAvatarIntent
import bose.ankush.home.presentation.account.AccountAvatarViewModel
import bose.ankush.home.presentation.ai.AiSummaryIntent
import bose.ankush.home.presentation.ai.AiSummaryViewModel
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyViewModel
import bose.ankush.home.presentation.places.HomeProgress
import bose.ankush.home.presentation.places.SavedPlacesEffect
import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.SavedPlacesViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

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
    shellPager: PagerState = rememberPagerState(pageCount = { SHELL_PAGE_COUNT }),
) {
    val places = sections.places
    val pager = rememberPagerState(pageCount = { HOME_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    // How to close whatever covers the hub page (plan sheet, web page); null when nothing does.
    var hubCover by remember { mutableStateOf<(() -> Unit)?>(null) }
    val onHub = shellPager.currentPage == HUB_PAGE
    var openAlert by remember { mutableStateOf<WeatherForecast.Alert?>(null) }
    var showAiSummary by remember { mutableStateOf(false) }
    val aiSummary = sections.aiSummary
    val closeAiSummary = {
        showAiSummary = false
        aiSummary.onDismiss()
    }
    val selectedTab =
        when {
            onHub -> HomeTab.HUB
            pager.currentPage == PLACES_PAGE -> HomeTab.MAP
            else -> HomeTab.HOME
        }
    val showPage = { page: Int ->
        scope.launch { pager.animateScrollToPage(page) }
        Unit
    }
    // From the hub, the inner page is set first, so the shell slides straight onto it.
    val showHomePage = { page: Int ->
        scope.launch {
            if (shellPager.currentPage == HUB_PAGE) {
                pager.scrollToPage(page)
                shellPager.animateScrollToPage(HOME_SHELL_PAGE)
            } else {
                pager.animateScrollToPage(page)
            }
        }
        Unit
    }
    val onTab = { tab: HomeTab ->
        openAlert = null
        closeAiSummary()
        when (tab) {
            HomeTab.HOME -> showHomePage(WEATHER_PAGE)
            HomeTab.MAP -> showHomePage(PLACES_PAGE)
            HomeTab.HUB -> scope.launch { shellPager.animateScrollToPage(HUB_PAGE) }
        }
        Unit
    }
    // Back on the places page returns to the weather page; open sheets handle back first.
    BackHandler(enabled = pager.currentPage == PLACES_PAGE && !onHub) { showPage(WEATHER_PAGE) }
    // Back on the hub returns to whichever home page it came from.
    BackHandler(enabled = onHub) { scope.launch { shellPager.animateScrollToPage(HOME_SHELL_PAGE) } }
    // Registered last so it wins: back closes what covers the hub before leaving it.
    BackHandler(enabled = hubCover != null) { hubCover?.invoke() }
    LaunchedEffect(places.effects) {
        places.effects.collect { effect ->
            when (effect) {
                SavedPlacesEffect.ShowPlaces -> pager.scrollToPage(PLACES_PAGE)
            }
        }
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage == PLACES_PAGE) places.onIntent(SavedPlacesIntent.PageShown)
    }

    val condition = shell.content.condition
    val photoUrl = shell.photo?.imageUrl
    val sample = rememberBackgroundImageSample(condition, photoUrl)
    // The weather page sits on the photo; the places map and the hub are dark, so their type is white.
    val contentColor = contentColorForLuminance(sample.headerLuminance)
    // Only the header and tab bar sit over both, so only they fade between the two. They read the
    // colour through a lambda, so the fade recomposes them alone, never the pager pages.
    val chromeColorState =
        animateColorAsState(
            targetValue = if (selectedTab == HomeTab.HOME) contentColor else NightContentColor,
            animationSpec = tween(durationMillis = CONTENT_COLOR_FADE_MILLIS),
            label = "homeChromeColor",
        )
    val chromeColor = remember(chromeColorState) { { chromeColorState.value } }
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

    val homeModel =
        HomePagerModel(
            shell = shell,
            sections = sections,
            contentColor = contentColor,
            chromeColor = chromeColor,
            chipColors = rememberMetricChipColors(sample.averageColor),
            cornerGlow = cornerGlowColor(sample.averageColor, sample.headerLuminance),
            openAlert = openAlert,
            aiSummaryVisible = showAiSummary,
            pager = pager,
            shellPager = shellPager,
        )
    val onHubCoverChange = remember { { close: (() -> Unit)? -> hubCover = close } }

    Box(modifier = modifier.fillMaxSize()) {
        // Behind both shell pages, as on iOS: the hub slides over the same photo, not its own fill.
        SkyConditionBackground(condition = condition, photoUrl = photoUrl)
        CornerGlow(color = homeModel.cornerGlow)
        PlacesMapLayer(pager = pager, shellPager = shellPager, places = places)
        // Outer pager: home (itself the weather and places pager) and the hub. Swiping on from the
        // places page carries on into the hub; on the weather page the shell stays put, so a
        // sideways drag on the header doesn't skip the places page.
        HorizontalPager(
            state = shellPager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = hubCover == null && (onHub || pager.currentPage == PLACES_PAGE),
        ) { page ->
            when (page) {
                HUB_PAGE -> HubPage(
                    hub = links.hub,
                    shellPager = shellPager,
                    onCoverChange = onHubCoverChange
                )

                else -> HomePager(
                    model = homeModel,
                    actions = actions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        AnimatedVisibility(
            visible = hubCover == null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = TabBarBottomGap),
        ) {
            HomeTabs(selected = selectedTab, onSelected = onTab, inactiveTint = chromeColor)
        }
    }
}

/** [HomeTabBar] reading its tint here, so a tint fade recomposes only the tab bar. */
@Composable
private fun HomeTabs(
    selected: HomeTab,
    onSelected: (HomeTab) -> Unit,
    inactiveTint: () -> Color,
    modifier: Modifier = Modifier,
) {
    HomeTabBar(
        selected = selected,
        onSelected = onSelected,
        inactiveTint = inactiveTint(),
        modifier = modifier
    )
}

private data class HomePagerModel(
    val shell: HomeScreenShell,
    val sections: HomeScreenSections,
    /** Type colour on the weather page, picked from the photo. */
    val contentColor: Color,
    /** Type colour for the header, which sits over the photo and the map alike. */
    val chromeColor: () -> Color,
    val chipColors: MetricChipColors,
    val cornerGlow: Color,
    val openAlert: WeatherForecast.Alert?,
    val aiSummaryVisible: Boolean,
    val pager: PagerState,
    /** The outer pager: home, then the hub. */
    val shellPager: PagerState,
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
        PullToRefreshBox(
            // Never held open: the pull plays its slide-down and lets go; the bottom loader shows the work.
            isRefreshing = false,
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
                            contentColor = model.chromeColor,
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
                                        contentColor = NightContentColor,
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
        // One bottom stack, centred: the loader rides above the toast while the toast shows.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RefreshChip(visible = chrome.loading, colors = model.chipColors)
            OfflineToast(chrome.offlineMessage)
        }
    }
}

/**
 * The map behind the places page, fixed behind both shell pages like the photo. It fades in
 * with the swipe onto places and out again on the swipe to the hub, so neither swipe shows a
 * hard edge where the map ends. Building it means about 40 tile images at once, so it waits
 * for the pager to rest ([rememberBuiltAtRest]).
 */
@Composable
private fun PlacesMapLayer(
    pager: PagerState,
    shellPager: PagerState,
    places: SavedPlacesBinding,
) {
    if (!rememberBuiltAtRest(pager = pager, page = PLACES_PAGE)) return
    // Flips only when the map starts or stops showing, so the pulse runs only while it can be
    // seen: not on the weather page, and not once the hub has slid over it.
    val visible by remember(pager, shellPager) {
        derivedStateOf {
            placesPageProgress(pager) > 0f &&
                    (shellPager.currentPage == HOME_SHELL_PAGE || shellPager.currentPageOffsetFraction != 0f)
        }
    }
    PlacesMapBackdrop(
        center = places.mapCenter,
        places = places.state.places,
        visible = visible,
        // Faded every frame of both swipes. Modulating each tile's alpha skips the full-screen
        // offscreen pass a layer alpha costs; the tiles abut, so nothing overlaps to show through.
        modifier =
            Modifier.graphicsLayer {
                alpha = placesPageProgress(pager) * (1f - hubPageProgress(shellPager))
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    )
}

/** 0 on the home page of the shell, 1 on the hub, in between mid-swipe. */
private fun hubPageProgress(shellPager: PagerState): Float =
    (shellPager.currentPage - HOME_SHELL_PAGE + shellPager.currentPageOffsetFraction).coerceIn(
        0f,
        1f
    )

/** 0 on the weather page, 1 on the places page, in between mid-swipe. */
private fun placesPageProgress(pager: PagerState): Float =
    (pager.currentPage - WEATHER_PAGE + pager.currentPageOffsetFraction).coerceIn(0f, 1f)

/** Observed time, location chip, and header. Stays above the pager on both pages. */
@Composable
private fun HomeHeaderBlock(
    content: HomeWeatherContent,
    chrome: HomeScreenChrome,
    nearby: NearbyContent,
    contentColor: () -> Color,
    modifier: Modifier = Modifier,
) {
    val color = contentColor()
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(28.dp))
        LocationOverrideChip(
            name = chrome.locationOverrideName,
            resetting = chrome.resettingLocation,
            onReset = chrome.onResetLocation,
            contentColor = color,
        )
        chrome.current?.dt?.let { observed ->
            AnimatedValueText(
                text = observedLabel(observed, forecastZone(chrome.timezoneOffset)),
                color = color.copy(alpha = OBSERVED_ALPHA),
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
            contentColor = color,
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
    // Hoisted here so the account mark and "upgrade" can slide the shell to the hub.
    val shellPager = rememberPagerState(pageCount = { SHELL_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val openHub = remember(
        shellPager,
        scope
    ) { { scope.launch { shellPager.animateScrollToPage(HUB_PAGE) }; Unit } }
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val nearbyState by nearbyViewModel.state.collectAsStateWithLifecycle()
    val avatarState by avatarViewModel.state.collectAsStateWithLifecycle()
    val placesState by placesViewModel.state.collectAsStateWithLifecycle()
    val aiSummaryState by aiSummaryViewModel.state.collectAsStateWithLifecycle()
    CollectHomeEffects(homeViewModel)
    LaunchedEffect(Unit) {
        avatarViewModel.onIntent(AccountAvatarIntent.Refresh)
    }
    // Lets the host drop the splash once a frame with the cached forecast has been drawn.
    LaunchedEffect(state.hasCheckedCache) {
        if (state.hasCheckedCache) {
            withFrameNanos {}
            HomeFirstContent.markReady()
        }
    }
    val placePoint = state.place?.point
    LaunchedEffect(placePoint) {
        nearbyViewModel.onIntent(NearbyIntent.LocationChanged(placePoint ?: return@LaunchedEffect))
    }
    SideEffect { Log.d(PERF_TAG, "HomeScreenRoute recomposed") } // TEMP(perf)

    // The mappings below are pure functions of the forecast, so they are remembered per forecast
    // instead of re-run on every recomposition (places, nearby and AI state all recompose this body).
    val weather = state.weatherData
    val place = state.place?.name ?: CURRENT_LOCATION
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
    // The photo follows the part of the day at the forecast place, so the clock ticks on its hours.
    val timezoneOffset = weather?.timezoneOffset
    val now by produceState(initialValue = Clock.System.now(), timezoneOffset) {
        hourTicks(forecastZone(timezoneOffset)).collect { value = it }
    }
    val backdropSlot =
        remember(content.condition, weatherMain, timezoneOffset, now) {
            backdropSlot(content.condition, weatherMain, timezoneOffset, now)
        }
    val photo = rememberBackgroundPhoto(backdropSlot)
    val statusMessage =
        (state.error ?: state.offlineMessage)?.takeIf { liveContent == null && it.isNotBlank() }
    val shell = remember(content, photo, statusMessage) { HomeScreenShell(content, photo, statusMessage) }
    val placeName = state.place?.name
    LaunchedEffect(placeName) {
        if (!placeName.isNullOrBlank()) {
            nearbyViewModel.onIntent(NearbyIntent.PlaceNameChanged(placeName))
        }
    }
    val nearby =
        remember(nearbyState, avatarState.photoUrl, openHub, nearbyViewModel) {
            nearbyState.toNearbyContent(
                photoUrl = avatarState.photoUrl,
                onOpenAccount = openHub,
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
    val homeProgress =
        HomeProgress(
            place = state.place,
            isLoading = state.isLoading,
            failed = state.notice != null
        )
    val places =
        remember(placesState, placesViewModel, openHub, placePoint, homeProgress) {
            SavedPlacesBinding(
                state = placesState,
                effects = placesViewModel.effect,
                onIntent = placesViewModel::processIntent,
                onUpgrade = openHub,
                mapCenter = placePoint,
                home = homeProgress,
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
            shellPager = shellPager,
        )
    }
}

private const val CURRENT_LOCATION = "Current Location"
private const val PERF_TAG = "HomePerf" // TEMP(perf)
private const val OBSERVED_ALPHA = 0.72f
private const val CONTENT_COLOR_FADE_MILLIS = 300

/** Type on the dark pages: the places map and the hub. */
private val NightContentColor = Color.White
private const val HOME_PAGE_COUNT = 2
private const val WEATHER_PAGE = 0
private const val PLACES_PAGE = 1
internal val TabBarHeight = 64.dp
internal val TabBarBottomGap = 12.dp
internal val TabBarReserveHeight = TabBarHeight + TabBarBottomGap
internal val HeaderTextShadow =
    Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 8f)
