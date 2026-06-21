package bose.ankush.weatherify.presentation.strings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bose.ankush.language.presentation.LanguageScreenStrings
import bose.ankush.weatherify.R

@Composable
fun rememberLanguageScreenStrings(): LanguageScreenStrings {
    val languageSelectedTemplate = stringResource(R.string.language_selected)
    return LanguageScreenStrings(
        screenTitle = stringResource(R.string.language_screen_title),
        screenSubtitle = stringResource(R.string.language_screen_subtitle),
        navigateBack = stringResource(R.string.language_navigate_back),
        languageSelected = { language -> languageSelectedTemplate.format(language) },
    )
}
