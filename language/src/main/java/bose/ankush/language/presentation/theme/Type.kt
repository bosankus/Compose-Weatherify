package bose.ankush.language.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bose.ankush.language.R

val SFCompactDisplay = FontFamily(
    Font(R.font.sf_compact_display_medium),
    Font(R.font.sf_compact_display_thin)
)

val SFCompactDisplayTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = SFCompactDisplay,
        fontWeight = FontWeight.Thin,
        fontSize = 60.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SFCompactDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SFCompactDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SFCompactDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SFCompactDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
)