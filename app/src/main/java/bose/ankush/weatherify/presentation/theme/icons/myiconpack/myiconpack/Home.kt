package bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.MyIconPack

val MyIconPack.Home: ImageVector
    get() {
        if (_home != null) {
            return _home!!
        }
        _home = Builder(
            name = "Home", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 1.5f, strokeLineCap = Round, strokeLineJoin =
                StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = NonZero
            ) {
                moveTo(18.4384f, 20.0f)
                curveTo(19.3561f, 20.0f, 20.1493f, 19.3726f, 20.2725f, 18.4632f)
                curveTo(20.3895f, 17.5988f, 20.5f, 16.4098f, 20.5f, 15.0f)
                curveTo(20.5f, 12.0f, 20.6683f, 10.1684f, 17.5f, 7.0f)
                curveTo(16.0386f, 5.5387f, 14.4064f, 4.419f, 13.3024f, 3.7409f)
                curveTo(12.4978f, 3.2466f, 11.5021f, 3.2466f, 10.6975f, 3.7409f)
                curveTo(9.5935f, 4.419f, 7.9613f, 5.5387f, 6.5f, 7.0f)
                curveTo(3.3316f, 10.1684f, 3.5f, 12.0f, 3.5f, 15.0f)
                curveTo(3.5f, 16.4098f, 3.6104f, 17.5988f, 3.7274f, 18.4631f)
                curveTo(3.8506f, 19.3726f, 4.6438f, 20.0f, 5.5615f, 20.0f)
                horizontalLineTo(18.4384f)
                close()
            }
        }
            .build()
        return _home!!
    }

private var _home: ImageVector? = null
