package bose.ankush.language.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.LightSystemBarIcons
import bose.ankush.commonui.theme.NightBackdrop
import bose.ankush.commonui.theme.NightCardFill
import bose.ankush.commonui.theme.NightCardStroke
import bose.ankush.commonui.theme.NightInk
import bose.ankush.commonui.theme.NightInkFaint
import bose.ankush.commonui.theme.NightInkMuted
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.language.generated.resources.Res
import bose.ankush.language.generated.resources.language_navigate_back
import bose.ankush.language.generated.resources.language_screen_subtitle
import bose.ankush.language.generated.resources.language_screen_title
import bose.ankush.language.generated.resources.language_selected
import bose.ankush.language.util.LocaleHelper.changeLanguageTo
import bose.ankush.language.util.LocaleHelper.getCountryFlag
import bose.ankush.language.util.LocaleHelper.getDefaultLanguage
import bose.ankush.language.util.LocaleHelper.getDisplayName
import bose.ankush.language.util.customAppLocale
import bose.ankush.language.util.matchLanguage
import org.jetbrains.compose.resources.stringResource

/**
 * Language picker, in the same night look as the profile screen it is opened from: round back
 * button, title and subtitle, and one rounded card listing the languages with the active one
 * ticked. Picking a language applies it at once.
 */
@Composable
fun LanguageScreen(
    languages: List<String>,
    navAction: () -> Unit,
) {
    val selected =
        remember(languages) {
            mutableStateOf(matchLanguage(languages, customAppLocale ?: getDefaultLanguage()))
        }
    val reveal = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { reveal.targetState = true }

    LightSystemBarIcons()
    Box(modifier = Modifier.fillMaxSize().background(NightBackdrop)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            item { BackButton(onClick = navAction, modifier = Modifier.padding(top = 8.dp)) }
            item { Header(modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)) }
            item {
                AnimatedVisibility(
                    visibleState = reveal,
                    enter =
                        fadeIn(tween(REVEAL_MILLIS)) +
                                slideInVertically(tween(REVEAL_MILLIS)) { it / REVEAL_OFFSET_DIVISOR },
                ) {
                    LanguageCard(languages = languages, selected = selected)
                }
            }
            item {
                Spacer(
                    modifier = Modifier.height(24.dp)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.language_navigate_back)
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NightInkFaint)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = NightInk,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.language_screen_title),
            color = NightInk,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 32.sp,
        )
        Text(
            text = stringResource(Res.string.language_screen_subtitle),
            color = NightInkMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
    }
}

/** One card, rows separated by hairlines, like the profile screen's sections. */
@Composable
private fun LanguageCard(
    languages: List<String>,
    selected: MutableState<String?>,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(NightCardFill)
                .border(1.dp, NightCardStroke, CardShape),
    ) {
        languages.forEachIndexed { index, language ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 70.dp),
                    thickness = 1.dp,
                    color = NightCardStroke
                )
            }
            LanguageRow(
                language = language,
                isSelected = selected.value == language,
                onSelect = {
                    // The platform may echo the tag back in another form (iw-IL as he-IL).
                    selected.value =
                        matchLanguage(languages, changeLanguageTo(language)) ?: language
                    customAppLocale = language
                },
            )
        }
    }
}

@Composable
private fun LanguageRow(
    language: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val displayName = remember(language) { language.getDisplayName() }
    val flag = remember(language) { language.getCountryFlag() }
    val fill by animateColorAsState(
        targetValue = if (isSelected) WarningYellow.copy(alpha = SELECTED_FILL_ALPHA) else Color.Transparent,
        animationSpec = tween(SELECT_MILLIS),
        label = "languageRowFill",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(fill)
                .semantics { selected = isSelected }
                .clickable(role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(NightInkFaint),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = flag, fontSize = 20.sp)
        }
        Text(
            text = displayName,
            color = NightInk,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(SELECT_MILLIS)) + scaleIn(
                tween(SELECT_MILLIS),
                initialScale = CHECK_START_SCALE
            ),
            exit = scaleOut(tween(SELECT_MILLIS), targetScale = CHECK_START_SCALE),
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(WarningYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(Res.string.language_selected, displayName),
                    tint = ToastOnWarning,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private val CardShape: Shape = RoundedCornerShape(20.dp)
private const val REVEAL_MILLIS = 360
private const val REVEAL_OFFSET_DIVISOR = 6
private const val SELECT_MILLIS = 180
private const val SELECTED_FILL_ALPHA = 0.10f
private const val CHECK_START_SCALE = 0.6f
