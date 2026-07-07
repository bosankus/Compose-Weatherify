package bose.ankush.home.presentation.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.error_icon_content
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.ic_error
import bose.ankush.home.generated.resources.retry_btn_txt
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ErrorBackgroundAnimation() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    )
}

@Composable
internal fun ShowError(
    modifier: Modifier,
    msg: String?,
    buttonText: String = stringResource(Res.string.retry_btn_txt),
    isLoading: Boolean = false,
    buttonAction: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_error),
                contentDescription = stringResource(Res.string.error_icon_content),
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.padding(top = 16.dp))

            Text(
                text = msg ?: stringResource(Res.string.general_error_txt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Button(
                onClick = buttonAction,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                modifier = Modifier.padding(top = 16.dp),
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = buttonText)
                    }
                } else {
                    Text(text = buttonText)
                }
            }
        }
    }
}
