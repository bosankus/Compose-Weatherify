package bose.ankush.weatherify.presentation.cities.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.R

@Composable
fun CityListItem(
    cityNameList: List<CityName>,
    position: Int,
    onItemClick: (Int) -> Unit
) {

    // it will be large and clear.
    // selected city name will have bordered box in AccentColor

    Text(
        text = cityNameList[position].name ?: stringResource(id = R.string.not_available),
        style = MaterialTheme.typography.h3,
        color = Color.White,
        modifier = Modifier
            .padding(all = 16.dp)
            .clickable { onItemClick(position) }
    )

}