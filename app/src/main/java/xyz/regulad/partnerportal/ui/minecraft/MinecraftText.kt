package xyz.regulad.partnerportal.ui.minecraft

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import xyz.regulad.partnerportal.R

val minecraftFontId: Int = R.font.minecraftia
const val minecraftPixelHeight = 8 // height in "minecraft pixels" of the font

val minecraftShadowColor = Color(0xFF3F3F3F)

val minecraftFontFamily = FontFamily(
    Font(minecraftFontId, FontWeight.Normal)
)

@Composable
fun TextStyleForFontSize(fontSize: TextUnit, textColor: Color = Color.White): TextStyle {
    with(LocalDensity.current) {
        val minecraftFontOffsetTextSize = fontSize / minecraftPixelHeight
        val minecraftFontOffset = Offset(minecraftFontOffsetTextSize.toPx(), minecraftFontOffsetTextSize.toPx())

        return TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontFamily = minecraftFontFamily,
            shadow = Shadow(
                color = minecraftShadowColor,
                offset = minecraftFontOffset,
                blurRadius = 0.1f // just needs to be non-zero; broken at 0 shadow
            ),
            lineHeight = fontSize * 1.25f
        )
    }
}

@Composable
fun MinecraftText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    color: Color = Color.White,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val textStyle = TextStyleForFontSize(fontSize, color)

    Text(
        text = text,
        modifier = modifier,
        style = textStyle,
        onTextLayout = onTextLayout,
    )
}

@Preview
@Composable
private fun MinecraftTextPreview() {
    Box(
        modifier = Modifier.background(Color.Black)
    ) {
        MinecraftText("Sample Text")
    }
}
