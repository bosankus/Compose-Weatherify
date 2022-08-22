package bose.ankush.weatherify.presentation.home.component

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.PermissionManager
import bose.ankush.weatherify.presentation.ui.theme.DefaultCardBackgroundLightGrey
import bose.ankush.weatherify.presentation.ui.theme.RedError
import bose.ankush.weatherify.presentation.ui.theme.TextWhite
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AirQualityLayout(
    heading: String = "Sample heading",
    content: String = "And some little bit of lulu content her eto show the UI and test it. huha!"
) {
    val context: Context = LocalContext.current

    val permissionList = listOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    )

    PermissionManager.RequestPermission(
        permissions = permissionList,
        onPermissionGranted = {
            Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
        }
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(all = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DefaultCardBackgroundLightGrey)
            .padding(horizontal = 15.dp, vertical = 20.dp)
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(weight = 1f, fill = false)
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.body2,
                color = Color.White.copy(0.6f),
            )
            Text(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .heightIn(50.dp),
                text = content,
                style = MaterialTheme.typography.body1,
                color = TextWhite,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_alert),
            contentDescription = "Play icon button",
            modifier = Modifier
                .size(46.dp)
                .padding(start = 16.dp),
            tint = RedError
        )
    }
}