package bose.ankush.home.presentation.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Not in the reference image. Same dark rounded card as the Wander rows.
 * Date is collected because [startsAt] has to be an ISO-8601 instant.
 *
 * iOS does not compose this. Android Wander is the live caller.
 */
@Composable
internal fun ShellCreateDialog(
    state: ShellState,
    canSave: Boolean,
    onIntent: (ShellIntent) -> Unit,
    onSave: () -> Unit,
) {
    if (!state.showCreate) return
    AlertDialog(
        onDismissRequest = { onIntent(ShellIntent.DismissCreate) },
        containerColor = DialogFill,
        titleContentColor = DialogInk,
        textContentColor = DialogInk,
        shape = RoundedCornerShape(CardRadius),
        title = {
            Text(
                text = "New event",
                color = DialogInk,
                fontSize = TitleSize,
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FieldGap),
            ) {
                Text(
                    text = "Nothing is saved until a title and a time are entered.",
                    color = DialogInk,
                    fontSize = BodySize,
                    lineHeight = BodyLine,
                )
                EventField(
                    value = state.title,
                    onValueChange = { onIntent(ShellIntent.TitleChanged(it)) },
                    label = "Title",
                )
                EventField(
                    value = state.dateText,
                    onValueChange = { onIntent(ShellIntent.DateChanged(it)) },
                    label = "Date (yyyy-MM-dd)",
                )
                EventField(
                    value = state.timeText,
                    onValueChange = { onIntent(ShellIntent.TimeChanged(it)) },
                    label = "Time (HH:mm)",
                )
                if (state.placeName.isNullOrBlank()) {
                    Text(
                        text = "Current place isn't available yet.",
                        color = DialogInk,
                        fontSize = BodySize,
                    )
                }
                state.createError?.let {
                    Text(
                        text = it,
                        color = DialogInk,
                        fontSize = BodySize,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = canSave && !state.posting,
                colors = dialogButtonColors(),
            ) {
                Text(text = "Save", fontSize = BodySize, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onIntent(ShellIntent.DismissCreate) },
                colors = dialogButtonColors(),
            ) {
                Text(text = "Cancel", fontSize = BodySize, fontWeight = FontWeight.Medium)
            }
        },
    )
}

@Composable
private fun EventField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = LabelSize) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(color = DialogInk, fontSize = BodySize),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = DialogInk,
                unfocusedTextColor = DialogInk,
                disabledTextColor = DialogInk.copy(alpha = MUTED_ALPHA),
                focusedBorderColor = DialogInk.copy(alpha = FOCUSED_BORDER_ALPHA),
                unfocusedBorderColor = DialogInk.copy(alpha = BORDER_ALPHA),
                disabledBorderColor = DialogInk.copy(alpha = BORDER_ALPHA),
                focusedLabelColor = DialogInk,
                unfocusedLabelColor = DialogInk.copy(alpha = LABEL_ALPHA),
                disabledLabelColor = DialogInk.copy(alpha = MUTED_ALPHA),
                cursorColor = DialogInk,
                focusedContainerColor = FieldFill,
                unfocusedContainerColor = FieldFill,
                disabledContainerColor = FieldFill,
            ),
    )
}

@Composable
private fun dialogButtonColors() =
    ButtonDefaults.textButtonColors(
        contentColor = DialogInk,
        disabledContentColor = DialogInk.copy(alpha = MUTED_ALPHA),
    )

/** Same fill as the Wander cards, a touch more opaque so the fields stay readable. */
private val DialogFill = Color.Black.copy(alpha = DIALOG_FILL_ALPHA)

/** Same soft off-white as WanderOnDark. Not pure white. */
private val DialogInk = Color(0xFFE7E4DC)

private val FieldFill = Color.Black.copy(alpha = FIELD_FILL_ALPHA)

private val CardRadius = 20.dp
private val FieldGap = 10.dp
private val TitleSize = 16.sp
private val BodySize = 14.sp
private val BodyLine = 18.sp
private val LabelSize = 13.sp
private const val DIALOG_FILL_ALPHA = 0.72f
private const val FIELD_FILL_ALPHA = 0.22f
private const val FOCUSED_BORDER_ALPHA = 0.55f
private const val BORDER_ALPHA = 0.28f
private const val LABEL_ALPHA = 0.72f
private const val MUTED_ALPHA = 0.38f
