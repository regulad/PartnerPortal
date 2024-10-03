package xyz.regulad.partnerportal.util

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import xyz.regulad.partnerportal.R

val minecraftFontFamily = FontFamily(
    Font(R.font.minecraft, FontWeight.Normal)
)


@Composable
fun MinecraftText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 16.sp,
    shadowColor: Color = Color(0xFF3F3F3F),
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val context = LocalContext.current

    val density = LocalDensity.current.density
    val shadowOffset = 0.25f * density // 1dp offset for shadow

    Box(modifier = modifier) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontFamily = minecraftFontFamily,
            style = style,
            onTextLayout = onTextLayout,
            modifier = Modifier.drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        this.color = shadowColor.toArgb()
                        this.textSize = fontSize.toPx()
                        typeface = ResourcesCompat.getFont(context, R.font.minecraft)!!
                    }
                    canvas.nativeCanvas.drawText(
                        text,
                        shadowOffset * 2,
                        fontSize.toPx() - (shadowOffset * 5),
                        paint
                    )
                }
            }
        )
    }
}
