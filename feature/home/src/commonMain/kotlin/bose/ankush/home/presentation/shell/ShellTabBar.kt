package bose.ankush.home.presentation.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sizes and colors here were not measured from the poster.
 * The bar is a dark pill, a label under each icon, and a white circle on the selected icon.
 */
@Composable
internal fun ShellTabBar(
    selected: ShellTab,
    onSelected: (ShellTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(RoundedCornerShape(BarCorner))
                .background(BarColor),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }.clickable { onSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(IconCircle)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.label,
                        tint = if (isSelected) SelectedIcon else IdleIcon,
                    )
                }
                Text(text = tab.label, color = IdleIcon, fontSize = LabelSize)
            }
        }
    }
}

private fun ShellTab.icon(): ImageVector =
    when (this) {
        ShellTab.HOME -> Icons.Filled.Home
        ShellTab.SAVED -> Icons.Filled.Bookmark
        ShellTab.SETTINGS -> Icons.Filled.Settings
    }

private val BarHeight = 72.dp
private val BarCorner = 36.dp
private val IconCircle = 32.dp
private val LabelSize = 11.sp
private val BarColor = Color(0xF01C1E24)
private val SelectedIcon = Color(0xFF161616)
private val IdleIcon = Color.White.copy(alpha = 0.85f)
