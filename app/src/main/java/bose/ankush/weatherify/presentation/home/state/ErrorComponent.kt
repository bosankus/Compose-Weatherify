package bose.ankush.weatherify.presentation.home.state

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R

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
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = msg ?: stringResource(id = R.string.general_error_txt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = buttonAction,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(top = 16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    disabledElevation = 0.dp,
                    defaultElevation = 30.dp,
                    pressedElevation = 10.dp
                )
            ) {
                Text(text = buttonText, color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}