package xyz.regulad.partnerportal.ui.minecraft

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun NoisyBrush(color: Color, temperature: Double = 0.1): Brush {
    val bitmapDimension = LocalDensity.current.run {
        32.dp.toPx().toInt()
    }

    val noiseMask = remember(temperature) {
        val bitmapSize = 16 // Size of the noise texture
        val mask = Array(bitmapSize) { FloatArray(bitmapSize) }

        for (x in 0 until bitmapSize) {
            for (y in 0 until bitmapSize) {
                val noise = Math.random().toFloat()
                mask[x][y] = 1f + (noise - 0.5f) * temperature.toFloat()
            }
        }

        mask
    }

    return remember(color, noiseMask, bitmapDimension) {
        val bitmapSize = noiseMask.size
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)

        for (x in 0 until bitmapSize) {
            for (y in 0 until bitmapSize) {
                val adjustedNoise = noiseMask[x][y]
                val pixelColor = Color(
                    red = (color.red * adjustedNoise).coerceIn(0f, 1f),
                    green = (color.green * adjustedNoise).coerceIn(0f, 1f),
                    blue = (color.blue * adjustedNoise).coerceIn(0f, 1f),
                    alpha = color.alpha
                )
                bitmap.setPixel(x, y, pixelColor.toArgb())
            }
        }

        // Create the appearance of larger pixels in the texture
        val finalBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmapDimension,
            bitmapDimension,
            false
        )

        val shader = BitmapShader(finalBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        ShaderBrush(shader)
    }
}
