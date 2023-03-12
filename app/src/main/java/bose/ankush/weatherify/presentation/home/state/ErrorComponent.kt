package bose.ankush.weatherify.presentation.home.state

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.ui.theme.AccentColor
import bose.ankush.weatherify.presentation.ui.theme.RedError
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun ShowError(
    modifier: Modifier,
    msg: String?,
    buttonText: String = stringResource(id = R.string.go_back),
    buttonAction: () -> Unit
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
                modifier = Modifier.size(36.dp),
                tint = RedError
            )
            Text(
                text = msg ?: stringResource(id = R.string.general_error_txt),
                style = MaterialTheme.typography.h3,
                color = TextWhite,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = buttonAction,
                colors = ButtonDefaults.buttonColors(AccentColor),
                modifier = Modifier.padding(top = 16.dp),
                elevation = ButtonDefaults.elevation(
                    disabledElevation = 0.dp,
                    defaultElevation = 30.dp,
                    pressedElevation = 10.dp
                )
            ) {
                Text(text = buttonText, color = TextWhite)
            }
        }
    }
}