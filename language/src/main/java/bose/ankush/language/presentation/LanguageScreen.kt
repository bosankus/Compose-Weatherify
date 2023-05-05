package bose.ankush.language.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import bose.ankush.language.R
import bose.ankush.language.presentation.LocaleHelper.getDisplayName

const val LANGUAGE_ARGUMENT_KEY = "country_config"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    languages: Array<String>,
    navAction: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = { SettingsHeader(navAction) },
            content = { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding)
                ) {
                    LanguageChangeSetting(languages, navAction)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHeader(navAction: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.back_content_description),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.lib_screen_header),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { navAction.invoke() }
                    .padding(all = 3.dp)
            )
        }
    )
}


@Composable
fun LanguageChangeSetting(languages: Array<String>, navAction: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        items(languages.size) { position ->
            Text(
                text = languages[position].getDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        // change language
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(languages[position])
                        )
                        // pop backstack
                        navAction.invoke()
                    }
                    .padding(all = 5.dp)
            )
        }
    }
}