package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.ui.theme.RedError
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun ShowError(
    error: String?,
    modifier: Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_error),
                contentDescription = stringResource(id = R.string.error_icon_content),
                modifier = Modifier
                    .size(46.dp)
                    .padding(start = 16.dp),
                tint = RedError
            )
            Text(
                text = error ?: stringResource(id = R.string.general_error_txt),
                style = MaterialTheme.typography.h3,
                color = TextWhite,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}