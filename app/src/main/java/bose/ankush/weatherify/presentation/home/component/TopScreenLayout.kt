package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.language.presentation.LANGUAGE_ARGUMENT_KEY
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.domain.model.Country
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.navigation.Screen

@Composable
fun TopScreenLayout(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val cityName = viewModel.cityName.value ?: stringResource(id = R.string.not_available)
    val languageList = LocaleConfigMapper.getAvailableLanguagesFromJson(
        jsonFile = "countryConfig.json",
        context = LocalContext.current
    )
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /*Menu icon*/
        Icon(
            modifier = Modifier
                .width(28.dp)
                .height(28.dp)
                .clip(CircleShape)
                .clickable { }
                .padding(all = 3.dp),
            painter = painterResource(id = R.drawable.ic_menu_white),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.menu_icon_content),
        )
        /*Location name*/
        Text(
            modifier = Modifier
                .padding(start = 5.dp)
                .clip(RoundedCornerShape(15.dp))
                .clickable { navController.navigate(Screen.CitiesListScreen.route) }
                .padding(all = 3.dp),
            text = cityName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        /*Language change icon*/
        Icon(
            modifier = Modifier
                .width(28.dp)
                .height(28.dp)
                .clip(CircleShape)
                .clickable { navController.navigate(Screen.LanguageScreen.withArgs(languageList)) }
                .padding(all = 3.dp),
            painter = painterResource(id = R.drawable.ic_settings),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.language_icon_content),
        )
    }
}