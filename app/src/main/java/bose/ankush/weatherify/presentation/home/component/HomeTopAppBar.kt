package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    title: String,
    titleNavAction: () -> Unit,
    leftNavAction: () -> Unit,
    rightNavAction: () -> Unit,
) {
    /*App Bar*/
    CenterAlignedTopAppBar(
        title = {
            Text(
                modifier = Modifier
                    .padding(start = 5.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .clickable { titleNavAction.invoke() }
                    .padding(all = 3.dp),
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        navigationIcon = {
            Icon(
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .clickable { leftNavAction.invoke() }
                    .padding(all = 3.dp),
                painter = painterResource(id = R.drawable.ic_menu_white),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.menu_icon_content),
            )
        },
        actions = {
            Icon(
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .clickable { rightNavAction.invoke() }
                    .padding(all = 3.dp),
                painter = painterResource(id = R.drawable.ic_settings),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.menu_icon_content),
            )
        }
    )
}
