package bose.ankush.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    negativeButtonText: String = "Cancel",
    positiveButtonText: String = "Allow",
    positiveOnClick: () -> Unit,
    openDialogBox: MutableState<Boolean>,
) {
    Dialog(onDismissRequest = { openDialogBox.value = false }) {
        DialogBoxUi(
            modifier = modifier,
            icon = icon,
            headingText = headingText,
            descriptionText = descriptionText,
            positiveOnClick = positiveOnClick,
            negativeButtonText = negativeButtonText,
            positiveButtonText = positiveButtonText,
            openDialogBox = openDialogBox,
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
    openDialogBox: MutableState<Boolean>,
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
                TextButton(onClick = { openDialogBox.value = false }) {
                    Text(
                        text = negativeButtonText,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                    )
                }
                TextButton(onClick = {
                    positiveOnClick()
                    openDialogBox.value = false
                }) {
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