package xyz.regulad.partnerportal.ui.minecraft

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import coil.size.pxOrElse
import coil.transform.Transformation

@Composable
fun MinecraftBackgroundImage(fileName: String) {
    HandledAsyncImage(
        fileName = fileName,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

class IntegerScalingTransformation(override val cacheKey: String) : Transformation {
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val targetWidth = size.width.pxOrElse { input.width }
        val targetHeight = size.height.pxOrElse { input.height }

        val scaleX = targetWidth / input.width
        val scaleY = targetHeight / input.height
        val scale = minOf(scaleX, scaleY).coerceAtLeast(1)

        val newWidth = input.width * scale
        val newHeight = input.height * scale

        return Bitmap.createScaledBitmap(input, newWidth, newHeight, false)
    }
}

@Composable
fun HandledAsyncImage(fileName: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    val dataUrl = "file:///android_asset/$fileName"

    var modelBuilder = ImageRequest.Builder(context)
        .data(dataUrl)
        .crossfade(true)

    if (!fileName.endsWith(".gif")) {
        modelBuilder = modelBuilder.transformations(IntegerScalingTransformation(dataUrl))
    }

    AsyncImage(
        model = modelBuilder.build(),
        imageLoader = imageLoader,
        contentDescription = fileName,
        contentScale = contentScale,
        modifier = modifier
    )
}
