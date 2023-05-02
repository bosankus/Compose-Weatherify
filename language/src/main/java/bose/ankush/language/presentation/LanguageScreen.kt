package bose.ankush.language.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import bose.ankush.language.R

const val LANGUAGE_ARGUMENT_KEY = "country_config"

@Composable
fun LanguageScreen(
    languages: Array<String>,
    navController: NavController,
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            SettingsHeader(navController)

            LanguageChangeSetting(languages, navController)
        }
    }
}

@Composable
private fun SettingsHeader(navController: NavController) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 13.dp, end = 16.dp, top = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.lib_screen_header),
            modifier = Modifier
                .clip(CircleShape)
                .clickable { navController.popBackStack() }
                .padding(all = 3.dp)

        )
        Text(
            text = stringResource(id = R.string.back_content_description),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}


@Composable
fun LanguageChangeSetting(languages: Array<String>, navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        items(languages.size) {
            Text(
                text = languages[it],
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(languages[it])
                        )
                        navController.popBackStack()
                    }
                    .padding(all = 5.dp)
            )
        }
    }
}