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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/**
 * Android Wander home. Home is this shell. Weather and Map are the existing
 * forecast and saved-places screens. Hub leaves this shell for Settings.
 */
@Composable
fun WanderHomeScreen(
    links: WanderHomeLinks,
    fogPhoto: WanderFogPhoto?,
    mock: WanderHomeMock = WanderHomePreviewData.home,
    modifier: Modifier = Modifier,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(WanderTab.HOME.name) }
    val selectedTab = WanderTab.entries.firstOrNull { it.name == selectedTabName } ?: WanderTab.HOME

    val onTab = { tab: WanderTab ->
        when (tab) {
            WanderTab.HUB -> links.onOpenHub()
            WanderTab.TRAVEL -> links.onOpenTravel()
            else -> selectedTabName = tab.name
        }
    }

    if (selectedTab == WanderTab.HOME) {
        WanderHomePage(mock = mock, fogPhoto = fogPhoto, selectedTab = selectedTab, onTab = onTab, modifier = modifier)
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
    mock: WanderHomeMock,
    fogPhoto: WanderFogPhoto?,
    selectedTab: WanderTab,
    onTab: (WanderTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoUrl = fogPhoto?.imageUrl?.takeIf { mock.condition == WanderCondition.FOG }
    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = mock.condition, photoUrl = photoUrl)
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
                temperature = mock.temperature,
                place = mock.place,
                conditionLine = mock.condition.line,
            )
            Spacer(modifier = Modifier.height(28.dp))
            WanderMetricChips(feel = mock.feel, wind = mock.wind, uv = mock.uv)
            Spacer(modifier = Modifier.height(20.dp))
            WanderCalendarStrip()
            Spacer(modifier = Modifier.height(16.dp))
            WanderSmallCards()
            if (fogPhoto != null && photoUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                UnsplashCredit(fogPhoto)
            }
            Spacer(modifier = Modifier.weight(1f))
            WanderTabBar(selected = selectedTab, onSelected = onTab)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun UnsplashCredit(photo: WanderFogPhoto) {
    val linkStyle =
        TextLinkStyles(
            SpanStyle(
                color = Color.White.copy(alpha = 0.92f),
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
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 11.sp,
    )
}

/** App entry. The preview does not call this, so it never touches Koin or the network. */
@Composable
fun WanderHomeRoute(
    links: WanderHomeLinks,
    modifier: Modifier = Modifier,
) {
    WanderHomeScreen(
        links = links,
        fogPhoto = rememberWanderFogPhoto(),
        modifier = modifier,
    )
}
