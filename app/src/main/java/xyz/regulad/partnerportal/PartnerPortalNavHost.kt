package xyz.regulad.partnerportal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import xyz.regulad.partnerportal.navigation.MinecraftPortalLoadingPage

@Serializable
data object MinecraftPortalLoadingRoute

@Composable
fun PartnerPortalNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = MinecraftPortalLoadingRoute, modifier = modifier) {
        composable<MinecraftPortalLoadingRoute> {
            MinecraftPortalLoadingPage()
        }
    }
}
