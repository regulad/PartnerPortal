package xyz.regulad.partnerportal.navigation

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.lerp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import xyz.regulad.partnerportal.util.MinecraftText
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


@Preview
@Composable
fun MinecraftPortalLoadingPage(modifier: Modifier = Modifier) {
    MinecraftPortalImage()

    Box(modifier = modifier.fillMaxSize()) {
        MinecraftText(
            "Connecting to partner...",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun MinecraftPortalImage() {
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

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/portal.gif")
            .crossfade(true)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "Portal",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize()
    )
}
