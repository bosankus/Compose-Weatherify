package bose.ankush.language.presentation

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import bose.ankush.language.R
import bose.ankush.language.util.LocaleHelper.changeLanguageTo
import bose.ankush.language.util.LocaleHelper.getCountryFlag
import bose.ankush.language.util.LocaleHelper.getDefaultLanguage
import bose.ankush.language.util.LocaleHelper.getDisplayName

@Composable
fun LanguageScreen(
    languages: Array<String>,
    navAction: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = { ScreenHeader(navAction) },
            content = { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    ShowUI(languages = languages)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenHeader(navAction: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.lib_screen_header),
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
private fun ShowUI(languages: Array<String>) {
    val changedLanguage = remember { mutableStateOf(getDefaultLanguage()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        items(languages.size) { position ->
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        changedLanguage.value = changeLanguageTo(languages[position])
                        Log.d("LanguageScreen", "Language changed to: ${changedLanguage.value}")
                    }
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${languages[position].getCountryFlag()}    ${languages[position].getDisplayName()}",
                    fontFamily = FontFamily.Default,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            emojiSupportMatch = EmojiSupportMatch.None
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                )
                if (changedLanguage.value == languages[position]) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "${languages[position]} selected"
                    )
                }
            }
        }
    }
}
