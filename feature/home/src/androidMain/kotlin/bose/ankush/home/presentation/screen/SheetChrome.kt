package bose.ankush.home.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow

/*
 * Shared layout for the home screen's task sheets (add a place, new event): a title row with a
 * round close button over a hairline, a muted subtitle, pill-shaped fields, and one full-width
 * primary button at the bottom.
 */

/** Title row and its divider. Sits in [SheetPanel]'s header slot, under the drag handle. */
@Composable
internal fun SheetTitleBar(
    title: String,
    closeLabel: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = ContentOnDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        RoundIconButton(icon = Icons.Filled.Close, label = closeLabel, onClick = onClose)
    }
    HorizontalDivider(
        modifier = Modifier.padding(end = 12.dp),
        thickness = 1.dp,
        color = SheetHairline
    )
}

@Composable
internal fun SheetSubtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = SheetMutedText,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SheetFieldFill)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ContentOnDark,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** What a [SheetField] shows and how its keyboard behaves. */
internal class SheetFieldSpec(
    val placeholder: String,
    val leadingIcon: ImageVector,
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val keyboardActions: KeyboardActions = KeyboardActions.Default,
    val clearLabel: String? = null,
)

/** Pill text field. With [SheetFieldSpec.clearLabel] it grows a clear button while non-empty. */
@Composable
internal fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    spec: SheetFieldSpec,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(text = spec.placeholder) },
        leadingIcon = { Icon(imageVector = spec.leadingIcon, contentDescription = null) },
        trailingIcon =
            spec.clearLabel?.let { label ->
                {
                    ClearButton(
                        visible = value.isNotEmpty(),
                        label = label,
                        onClear = { onValueChange("") })
                }
            },
        keyboardOptions = spec.keyboardOptions,
        keyboardActions = spec.keyboardActions,
        shape = SheetFieldShape,
        colors = sheetFieldColors(),
    )
}

@Composable
private fun ClearButton(
    visible: Boolean,
    label: String,
    onClear: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CLEAR_ANIM_MILLIS)) + scaleIn(tween(CLEAR_ANIM_MILLIS)),
        exit = fadeOut(tween(CLEAR_ANIM_MILLIS)) + scaleOut(tween(CLEAR_ANIM_MILLIS)),
    ) {
        RoundIconButton(icon = Icons.Filled.Close, label = label, onClick = onClear)
    }
}

@Composable
private fun sheetFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = SheetFieldFill,
        unfocusedContainerColor = SheetFieldFill,
        focusedTextColor = ContentOnDark,
        unfocusedTextColor = ContentOnDark,
        focusedPlaceholderColor = SheetMutedText,
        unfocusedPlaceholderColor = SheetMutedText,
        focusedLeadingIconColor = ContentOnDark,
        unfocusedLeadingIconColor = SheetMutedText,
        focusedTrailingIconColor = SheetMutedText,
        unfocusedTrailingIconColor = SheetMutedText,
        cursorColor = WarningYellow,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

/** Full-width primary action. Muted until [enabled]; [busy] adds a spinner before the label. */
@Composable
internal fun SheetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SheetButtonHeight)
                .clip(SheetFieldShape)
                .background(if (enabled) WarningYellow else SheetFieldFill)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SheetMutedText,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.size(10.dp))
        }
        Text(
            text = text,
            color = if (enabled) ToastOnWarning else SheetMutedText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal val SheetFieldFill = ContentOnDark.copy(alpha = 0.08f)
internal val SheetFieldShape = RoundedCornerShape(16.dp)
private val SheetHairline = ContentOnDark.copy(alpha = 0.08f)
private val SheetButtonHeight = 52.dp
private const val CLEAR_ANIM_MILLIS = 150
