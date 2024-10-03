package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftText


@Preview
@Composable
fun LoadingPage(modifier: Modifier = Modifier) {
    MinecraftBackgroundImage("portal.gif")

    Box(modifier = modifier.fillMaxSize()) {
        MinecraftText(
            "Connecting to partner...",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

