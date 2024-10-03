package xyz.regulad.partnerportal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import xyz.regulad.partnerportal.navigation.LoadingPage
import xyz.regulad.partnerportal.navigation.StartupPage

@Serializable
data object LoadingRoute

@Serializable
data object StartupRoute

@Composable
fun PartnerPortalNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = StartupRoute, modifier = modifier) {
        composable<StartupRoute> {
            StartupPage()
        }

        composable<LoadingRoute> {
            LoadingPage()
        }
    }
}
