package bose.ankush.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bose.ankush.dialog.theme.AccentColor
import bose.ankush.dialog.theme.BackgroundGrey
import bose.ankush.dialog.theme.DefaultCardBackgroundLightGrey
import bose.ankush.dialog.theme.White

/**
 * A dialog that displays an icon, heading text, description texts as message and two buttons.
 */
@Composable
fun DialogBox(
    modifier: Modifier = Modifier,
    icon: Int = R.drawable.ic_info,
    headingText: String = "Dialog heading",
    descriptionText: String = "Dialog description text",
    confirmButtonText: String = "Allow",
    dismissButtonText: String = "Cancel",
    confirmOnClick: () -> Unit,
    closeOnClick: MutableState<Boolean>,
) {
    Dialog(
        onDismissRequest = { closeOnClick.value },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        DialogBoxUi(
            modifier = modifier,
            icon = icon,
            headingText = headingText,
            descriptionText = descriptionText,
            negativeButtonText = dismissButtonText,
            positiveButtonText = confirmButtonText,
            positiveOnClick = confirmOnClick,
            negativeOnClick = closeOnClick ,
        )
    }
}

@Composable
fun DialogBoxUi(
    modifier: Modifier,
    icon: Int,
    headingText: String,
    descriptionText: String,
    negativeButtonText: String,
    positiveButtonText: String,
    positiveOnClick: () -> Unit,
    negativeOnClick: MutableState<Boolean>,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(start = 10.dp, top = 5.dp, end = 10.dp, bottom = 10.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = modifier.background(BackgroundGrey)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(color = AccentColor),
                modifier = Modifier
                    .padding(top = 15.dp)
                    .height(30.dp)
                    .fillMaxWidth()
            )
            Column(
                modifier = Modifier.padding(all = 16.dp)
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth(),
                    text = headingText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 25.dp, end = 25.dp)
                        .fillMaxWidth(),
                    text = descriptionText,
                    color = White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(DefaultCardBackgroundLightGrey)
            ) {
                // Dismiss button
                TextButton(onClick = { negativeOnClick.value = false }) {
                    Text(
                        text = negativeButtonText,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                    )
                }
                // Confirm button
                TextButton(onClick = { positiveOnClick() }) {
                    Text(
                        text = positiveButtonText,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentColor,
                        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                    )
                }
            }
        }
    }
}
