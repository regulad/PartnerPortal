package xyz.regulad.partnerportal.ui.minecraft

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import xyz.regulad.partnerportal.R

val minecraftFontId: Int = R.font.minecraftia
const val minecraftPixelHeight = 8 // height in "minecraft pixels" of the font

val minecraftFontFamily = FontFamily(
    Font(minecraftFontId, FontWeight.Normal)
)

@Composable
fun MinecraftText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 16.sp,
    shadowColor: Color = Color(0xFF3F3F3F),
    style: TextStyle = TextStyle.Default,
    lineHeight: TextUnit = fontSize * 1.25f,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val context = LocalContext.current

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = minecraftFontFamily,
        style = style,
        lineHeight = lineHeight,
        onTextLayout = onTextLayout,
        modifier = modifier.drawBehind {
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    this.color = shadowColor.toArgb()
                    this.textSize = fontSize.toPx()
                    typeface = ResourcesCompat.getFont(context, minecraftFontId)!!
                }

                text.split("\n").forEachIndexed { index, line ->
                    val indexOffset = index * lineHeight.toPx()

                    canvas.nativeCanvas.drawText(
                        line,
                        (fontSize / minecraftPixelHeight).toPx(),
                        (2 * fontSize.toPx()) - (fontSize / minecraftPixelHeight).toPx() + indexOffset,
                        paint
                    )
                }
            }
        }
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
