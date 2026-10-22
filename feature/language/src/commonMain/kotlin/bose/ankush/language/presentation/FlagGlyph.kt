package bose.ankush.language.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.NightInkMuted
import bose.ankush.language.generated.resources.Res
import bose.ankush.language.generated.resources.flag_il
import bose.ankush.language.generated.resources.flag_in
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The flag for a language tag's region (`en-IN` → India), drawn as a vector so it looks the same
 * everywhere: flag emoji depend on the platform's emoji font, which Compose on iOS does not reach.
 * A region without a drawing shows its two-letter code. Fills its parent; the caller clips it.
 */
@Composable
internal fun FlagGlyph(
    languageTag: String,
    modifier: Modifier = Modifier,
) {
    val region = regionOf(languageTag)
    val flag = FLAGS[region]
    if (flag != null) {
        Image(
            painter = painterResource(flag),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Text(
            text = region,
            color = NightInkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The region subtag, upper-cased: `en-IN` → `IN`. Empty when the tag has none. */
internal fun regionOf(languageTag: String): String =
    languageTag
        .split('-', '_')
        .drop(1)
        .firstOrNull { it.length == 2 && it.all(Char::isLetter) }
        ?.uppercase()
        .orEmpty()

private val FLAGS: Map<String, DrawableResource> =
    mapOf(
        "IN" to Res.drawable.flag_in,
        "IL" to Res.drawable.flag_il,
    )
