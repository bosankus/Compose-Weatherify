package bose.ankush.home.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bottom tabs. One white pill sits behind the selected item. On a change its leading
 * edge moves first and the trailing edge follows, so the pill stretches and settles.
 */
@Composable
fun HomeTabBar(
    selected: HomeTab,
    onSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    inactiveTint: Color = inactiveIcon,
) {
    val tabs = HomeTab.entries
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(tabBarScrim)
                .padding(4.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / tabs.size
            val barWidth = maxWidth
            val lastIndex = remember { intArrayOf(selectedIndex) }
            val movingRight =
                remember(selectedIndex) {
                    val right = selectedIndex >= lastIndex[0]
                    lastIndex[0] = selectedIndex
                    right
                }
            // Leading edge springs ahead, trailing edge lags, so the pill stretches then settles.
            val left by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = if (movingRight) trailingEdge else leadingEdge,
                label = "homeTabPillLeft",
            )
            val right by animateDpAsState(
                targetValue = tabWidth * (selectedIndex + 1),
                animationSpec = if (movingRight) leadingEdge else trailingEdge,
                label = "homeTabPillRight",
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val start = left.toPx().coerceIn(0f, barWidth.toPx())
                            val end = right.toPx().coerceIn(start, barWidth.toPx())
                            val restWidth = tabWidth.toPx()
                            val stretch = ((end - start - restWidth) / restWidth).coerceIn(0f, 1f)
                            val pillHeight = size.height * (1f - PILL_MAX_SQUASH * stretch)
                            val radius = PILL_RADIUS.toPx().coerceAtMost(pillHeight / 2f)
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(start, (size.height - pillHeight) / 2f),
                                size = Size(end - start, pillHeight),
                                cornerRadius = CornerRadius(radius, radius),
                            )
                        },
            )
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = tab == selected
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) selectedIcon else inactiveTint,
                        animationSpec = tween(durationMillis = TINT_FADE_MILLIS),
                        label = "homeTabTint",
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics(mergeDescendants = true) {
                                    role = Role.Tab
                                    this.selected = isSelected
                                    contentDescription = tab.label
                                }.clickable(role = Role.Tab) { onSelected(tab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = tint,
                        )
                    }
                }
            }
        }
    }
}

private const val TINT_FADE_MILLIS = 220
private const val PILL_MAX_SQUASH = 0.08f
private const val PILL_DAMPING = 0.75f
private const val LEADING_STIFFNESS = 400f
private const val TRAILING_STIFFNESS = 200f
private val PILL_RADIUS = 28.dp
private val leadingEdge = spring<Dp>(dampingRatio = PILL_DAMPING, stiffness = LEADING_STIFFNESS)
private val trailingEdge = spring<Dp>(dampingRatio = PILL_DAMPING, stiffness = TRAILING_STIFFNESS)

private val tabBarScrim = Color(0x66101418)
private val selectedIcon = Color(0xFF161616)
private val inactiveIcon = Color.White.copy(alpha = 0.5f)
