package bose.ankush.weatherify.base.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopAppBar(
    headlineId: Int,
    navIconAction: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = headlineId),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.close_icon_content),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { navIconAction.invoke() }
                    .padding(all = 3.dp)
            )
        }
    )
}