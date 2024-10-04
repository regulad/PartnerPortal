package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.Serializable
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftButton
import xyz.regulad.partnerportal.ui.minecraft.MinecraftText
import xyz.regulad.partnerportal.util.navigateOneWay

@Serializable
data class ErrorRoute(
    val errorMessage: String
)

@Composable
fun ErrorPage(route: ErrorRoute, navController: NavController) {
    MinecraftBackgroundImage("dirt.png")

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MinecraftText(
                "Encountered an error: ${route.errorMessage}",
            )

            Spacer(modifier = Modifier.height(10.dp))

            MinecraftButton("Try Again") {
                navController.navigateOneWay(StartupRoute)
            }
        }
    }
}
