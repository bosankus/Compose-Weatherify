package bose.ankush.home.presentation.wander

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Bottom tabs. The white pill slides to the selected item.
 */
@Composable
fun WanderTabBar(
    selected: WanderTab,
    onSelected: (WanderTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = WanderTab.entries
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
            val pillOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = tween(durationMillis = PILL_SLIDE_MILLIS),
                label = "wanderTabPill",
            )
            Box(
                modifier =
                    Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = tab == selected
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics {
                                    role = Role.Tab
                                    this.selected = isSelected
                                }.clickable { onSelected(tab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) selectedIcon else inactiveIcon,
                        )
                    }
                }
            }
        }
    }
}

private const val PILL_SLIDE_MILLIS = 280

private val tabBarScrim = Color(0x66101418)
private val selectedIcon = Color(0xFF161616)
private val inactiveIcon = Color.White.copy(alpha = 0.5f)
