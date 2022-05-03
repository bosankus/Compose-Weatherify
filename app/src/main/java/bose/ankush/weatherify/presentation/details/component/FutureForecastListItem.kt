package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.presentation.ui.theme.AccentColor
import bose.ankush.weatherify.presentation.ui.theme.DefaultCardBackgroundLightGrey

@Composable
fun FutureForecastListItem(
    avgForecastList: List<AvgForecast>,
    onItemClick: (Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp)
    ) {
        items(avgForecastList.size) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        selectedItem = it
                        onItemClick(it)
                    }
                    .background(if (selectedItem == it) AccentColor else DefaultCardBackgroundLightGrey)
                    .padding(horizontal = 10.dp, vertical = 20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = avgForecastList[it].nameOfDay?.substring(0,3) ?: "--",
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier = Modifier.alpha(0.6f),
                    )
                    Text(
                        text = stringResource(
                            id = R.string.celsius,
                            avgForecastList[it].avgTemp ?: "--"
                        ),
                        style = MaterialTheme.typography.h6,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = stringResource(
                            id = R.string.feels_like,
                            avgForecastList[it].feelsLike ?: "--"
                        ),
                        style = MaterialTheme.typography.caption,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .alpha(0.6f),
                    )
                }
            }
        }
    }
}
