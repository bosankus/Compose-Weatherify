package bose.ankush.language.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bose.ankush.language.generated.resources.Res
import bose.ankush.language.generated.resources.language_navigate_back
import bose.ankush.language.generated.resources.language_screen_subtitle
import bose.ankush.language.generated.resources.language_screen_title
import bose.ankush.language.generated.resources.language_selected
import bose.ankush.language.util.LocaleHelper.changeLanguageTo
import bose.ankush.language.util.LocaleHelper.getCountryFlag
import bose.ankush.language.util.LocaleHelper.getDefaultLanguage
import bose.ankush.language.util.LocaleHelper.getDisplayName
import org.jetbrains.compose.resources.stringResource

private const val ITEM_STAGGER_DELAY_MS = 100L

@Composable
fun LanguageScreen(
    languages: List<String>,
    navAction: () -> Unit,
) {
    val screenTransitionState = remember { MutableTransitionState(false) }
    val rememberedNavAction = remember { navAction }
    // Hoist the changedLanguage state to prevent recreation in ShowUI
    val changedLanguage = remember { mutableStateOf(getDefaultLanguage()) }

    LaunchedEffect(Unit) {
        screenTransitionState.targetState = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = { ScreenHeader(rememberedNavAction) },
            content = { innerPadding ->
                AnimatedVisibility(
                    visibleState = screenTransitionState,
                    enter =
                        fadeIn(animationSpec = tween(durationMillis = 400)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 500),
                                initialOffsetY = { it / 3 },
                            ),
                    exit = fadeOut(),
                ) {
                    Column(modifier = Modifier.padding(innerPadding)) {
                        LanguageScreenHeader()

                        Spacer(modifier = Modifier.height(16.dp))

                        ShowUI(
                            languages = languages,
                            changedLanguage = changedLanguage,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun LanguageScreenHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.language_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.language_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenHeader(navAction: () -> Unit) {
    val headerTransitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        headerTransitionState.targetState = true
    }

    AnimatedVisibility(
        visibleState = headerTransitionState,
        enter =
            fadeIn(animationSpec = tween(durationMillis = 300)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 300),
                    initialOffsetY = { -it / 2 },
                ),
        exit = fadeOut(),
    ) {
        TopAppBar(
            title = { },
            navigationIcon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                    modifier =
                        Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { navAction.invoke() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(Res.string.language_navigate_back),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
        )
    }
}

@Composable
private fun ShowUI(
    languages: List<String>,
    changedLanguage: androidx.compose.runtime.MutableState<String>,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        state = listState,
    ) {
        itemsIndexed(
            items = languages,
            key = { _, item -> item },
        ) { _, language ->
            LanguageItem(
                language = language,
                isSelected = changedLanguage.value == language,
                onLanguageSelected =
                    remember(language) {
                        {
                            changedLanguage.value = changeLanguageTo(language)
                        }
                    },
            )
        }
    }
}

@Composable
private fun LanguageItem(
    language: String,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit,
) {
    val displayName = remember(language) { language.getDisplayName() }
    val countryFlag = remember(language) { language.getCountryFlag() }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onLanguageSelected),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                LanguageFlag(countryFlag)

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }

            if (isSelected) {
                SelectionCheckmark(language)
            }
        }
    }
}

@Composable
private fun LanguageFlag(countryFlag: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.size(40.dp),
    ) {
        Text(
            text = countryFlag,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun SelectionCheckmark(language: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            tint = MaterialTheme.colorScheme.onPrimary,
            contentDescription = stringResource(Res.string.language_selected, language),
            modifier = Modifier.padding(6.dp),
        )
    }
}
