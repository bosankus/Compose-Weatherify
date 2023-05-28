package bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.MyIconPack

val MyIconPack.Account: ImageVector
    get() {
        if (_account != null) {
            return _account!!
        }
        _account = Builder(
            name = "Account", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 128.0f, viewportHeight = 128.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(30.0f, 49.0f)
                curveToRelative(0.0f, 18.7f, 15.3f, 34.0f, 34.0f, 34.0f)
                reflectiveCurveToRelative(34.0f, -15.3f, 34.0f, -34.0f)
                reflectiveCurveTo(82.7f, 15.0f, 64.0f, 15.0f)
                reflectiveCurveTo(30.0f, 30.3f, 30.0f, 49.0f)
                close()
                moveTo(90.0f, 49.0f)
                curveToRelative(0.0f, 14.3f, -11.7f, 26.0f, -26.0f, 26.0f)
                reflectiveCurveTo(38.0f, 63.3f, 38.0f, 49.0f)
                reflectiveCurveToRelative(11.7f, -26.0f, 26.0f, -26.0f)
                reflectiveCurveTo(90.0f, 34.7f, 90.0f, 49.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(24.4f, 119.4f)
                curveTo(35.0f, 108.8f, 49.0f, 103.0f, 64.0f, 103.0f)
                reflectiveCurveToRelative(29.0f, 5.8f, 39.6f, 16.4f)
                lineToRelative(5.7f, -5.7f)
                curveTo(97.2f, 101.7f, 81.1f, 95.0f, 64.0f, 95.0f)
                reflectiveCurveToRelative(-33.2f, 6.7f, -45.3f, 18.7f)
                lineTo(24.4f, 119.4f)
                close()
            }
        }
            .build()
        return _account!!
    }

private var _account: ImageVector? = null
