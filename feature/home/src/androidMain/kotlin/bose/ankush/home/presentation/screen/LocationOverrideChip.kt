package bose.ankush.home.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.location_override_chip_content_desc
import bose.ankush.home.generated.resources.location_override_reset_btn
import bose.ankush.home.generated.resources.location_override_resetting
import org.jetbrains.compose.resources.stringResource

/**
 * "Showing <place> · Reset to GPS" pill above the header, shown while a saved place drives the
 * forecast. Tapping it turns the yellow pin into a spinner ("Switching to GPS…") until the GPS
 * forecast lands; then the pill folds away. It unfolds the same way when a place is pinned.
 * [name] null hides it.
 */
@Composable
internal fun LocationOverrideChip(
    name: String?,
    resetting: Boolean,
    onReset: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    // The last name, so the pill keeps its text while it animates out.
    var shownName by remember { mutableStateOf(name.orEmpty()) }
    if (name != null) shownName = name
    AnimatedVisibility(
        visible = name != null,
        modifier = modifier,
        enter =
            expandVertically(chipSpring(), expandFrom = Alignment.Top) +
                    fadeIn(tween(CHIP_FADE_MILLIS)) +
                    scaleIn(
                        chipSpring(),
                        initialScale = CHIP_HIDDEN_SCALE,
                        transformOrigin = TopStart
                    ),
        exit =
            shrinkVertically(tween(CHIP_EXIT_MILLIS), shrinkTowards = Alignment.Top) +
                    fadeOut(tween(CHIP_FADE_MILLIS)) +
                    scaleOut(
                        tween(CHIP_EXIT_MILLIS),
                        targetScale = CHIP_HIDDEN_SCALE,
                        transformOrigin = TopStart
                    ),
    ) {
        Column {
            ChipBody(
                name = shownName,
                resetting = resetting,
                onReset = onReset,
                contentColor = contentColor
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ChipBody(
    name: String,
    resetting: Boolean,
    onReset: () -> Unit,
    contentColor: Color,
) {
    val description = stringResource(Res.string.location_override_chip_content_desc, name)
    val resettingLabel = stringResource(Res.string.location_override_resetting)
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(ChipFill)
                .border(1.dp, ChipRim, CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = description
                    if (resetting) stateDescription = resettingLabel
                }.clickable(enabled = !resetting, onClick = onReset)
                .animateContentSize(chipSpring())
                .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).background(WarningYellow),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = resetting,
                transitionSpec = {
                    (fadeIn(tween(SWAP_MILLIS)) + scaleIn(
                        tween(SWAP_MILLIS),
                        initialScale = SWAP_SCALE
                    ))
                        .togetherWith(
                            fadeOut(tween(SWAP_MILLIS)) + scaleOut(
                                tween(SWAP_MILLIS),
                                targetScale = SWAP_SCALE
                            )
                        )
                },
                contentAlignment = Alignment.Center,
                label = "locationChipIcon",
            ) { busy ->
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = ToastOnWarning,
                        trackColor = Color.Transparent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ToastOnWarning,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = name,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 160.dp),
        )
        Box(
            modifier = Modifier.size(3.dp).clip(CircleShape)
                .background(contentColor.copy(alpha = DOT_ALPHA))
        )
        AnimatedContent(
            targetState = resetting,
            transitionSpec = {
                fadeIn(
                    tween(
                        SWAP_MILLIS,
                        delayMillis = SWAP_MILLIS / 2
                    )
                ).togetherWith(fadeOut(tween(SWAP_MILLIS / 2)))
            },
            label = "locationChipAction",
        ) { busy ->
            Text(
                text = if (busy) resettingLabel else stringResource(Res.string.location_override_reset_btn),
                color = contentColor.copy(alpha = ACTION_ALPHA),
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

private fun <T> chipSpring() =
    spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

private val TopStart = TransformOrigin(0f, 0f)
private val ChipFill = Color.Black.copy(alpha = 0.38f)
private val ChipRim = Color.White.copy(alpha = 0.14f)
private const val CHIP_FADE_MILLIS = 220
private const val CHIP_EXIT_MILLIS = 260
private const val CHIP_HIDDEN_SCALE = 0.9f
private const val SWAP_MILLIS = 180
private const val SWAP_SCALE = 0.6f
private const val DOT_ALPHA = 0.5f
private const val ACTION_ALPHA = 0.78f
