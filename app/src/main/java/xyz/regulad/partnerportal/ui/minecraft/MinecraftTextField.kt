package xyz.regulad.partnerportal.ui.minecraft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.regulad.partnerportal.MainActivity

@Composable
fun MinecraftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as MainActivity

    val topBottomPadding = 10.dp
    val leftRightPadding = 10.dp

    val minecraftFontSize = 16.sp
    // offset because shadow makes it larger than the font size
    val minecraftFontOffset = minecraftFontSize / minecraftPixelHeight

    val interactionSource = remember { MutableInteractionSource() }
    val isActive by interactionSource.collectIsFocusedAsState()

    var isUnderscoreFrame by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        while (isActive) {
            delay(500)
            isUnderscoreFrame = !isUnderscoreFrame
        }
    }

    Box(
        modifier = modifier
            .border(2.dp, textFieldBorderColor)
            .background(Color.Black)
            .padding(
                leftRightPadding,
                topBottomPadding,
                leftRightPadding - minecraftFontOffset.toDp(),
                topBottomPadding - minecraftFontOffset.toDp()
            ),
    ) {
        val textStyle = TextStyleForFontSize(minecraftFontSize)

        // idk why this is needed
        val cursorTextMeasurer = rememberTextMeasurer()

        BasicTextField(
            value.replace('\n', ' '),
            onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .drawWithContent {
                    drawContent()
                    // draw the cursor
                    if (isUnderscoreFrame && isActive) {
                        val drawnTextSize = cursorTextMeasurer.measure(value.replace('\n', ' '), softWrap = false, style = textStyle)
                        val xyOffset = Offset(
                            x = drawnTextSize.size.width.toFloat() + 1,
                            y = 0f
                        )

                        try {
                            drawText(
                                textMeasurer = cursorTextMeasurer,
                                text = "_",
                                topLeft = xyOffset,
                                style = textStyle
                            )
                        } catch (e: Exception) {
                            // ignore; we are more than likely just trying to draw the cursor at the end of the text
                        }
                    }
                },
            textStyle = textStyle,
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
            )
        )
    }
}

val textFieldBorderColor = Color(0xFFa0a0a0)
