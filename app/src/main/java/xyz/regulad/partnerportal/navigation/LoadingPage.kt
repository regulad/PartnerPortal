package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import xyz.regulad.partnerportal.PartnerPortalViewModel
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftText

@Serializable
data object LoadingRoute


@Composable
fun LoadingPage(viewModel: PartnerPortalViewModel) {
    val connectingStatus by viewModel.connectingStatus.collectAsState()

    MinecraftBackgroundImage("portal.gif")

    Box(modifier = Modifier.fillMaxSize()) {
        MinecraftText(
            connectingStatus,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
