package xyz.regulad.partnerportal.ui.minecraft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.regulad.partnerportal.ui.minecraft.ClickSoundManager.doClickSound

val buttonBackgroundColor = Color(0xFF8c8c8c)
val defaultTextColor = Color.White
val selectedTextColor = Color(0xFFffffa0)
val selectedButtonColor = Color(0xFF7d87be)

@Composable
fun TextUnit.toDp(): Dp {
    return LocalDensity.current.run {
        this@toDp.toDp()
    }
}

@Composable
fun MinecraftButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current

    val topBottomPadding = 10.dp
    val leftRightPadding = 30.dp

    val minecraftFontSize = 16.sp
    // offset because shadow makes it larger than the font size
    val minecraftFontOffset = minecraftFontSize / minecraftPixelHeight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val textColor = if (isPressed) selectedTextColor else defaultTextColor
    val buttonBackgroundColor = if (isPressed) selectedButtonColor else buttonBackgroundColor

    val backgroundBrush = NoisyBrush(buttonBackgroundColor)

    Surface(
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = interactionSource
        ) {
            context.doClickSound()
            onClick()
        }
    ) {
        Box(
            modifier = modifier
                .border(2.dp, Color.Black)
                .background(backgroundBrush)
                .padding(leftRightPadding, topBottomPadding, leftRightPadding- minecraftFontOffset.toDp(), topBottomPadding- minecraftFontOffset.toDp()),
        ) {
            MinecraftText(
                text = label,
                color = textColor,
            )
        }
    }
}

@Preview
@Composable
private fun MinecraftButtonPreview() {
    MinecraftButton("Sample Button")
}
