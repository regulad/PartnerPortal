package xyz.regulad.partnerportal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import xyz.regulad.partnerportal.navigation.*

@Composable
fun PartnerPortalNavHost(
    modifier: Modifier = Modifier,
    navHostController: NavHostController,
    viewModel: PartnerPortalViewModel
) {
    NavHost(navController = navHostController, startDestination = StartupRoute, modifier = modifier) {
        composable<StartupRoute> {
            StartupPage(
                viewModel = viewModel
            )
        }

        composable<LoadingRoute> {
            LoadingPage(viewModel = viewModel)
        }

        composable<ErrorRoute> { navBackStackEntry ->
            val route: ErrorRoute = navBackStackEntry.toRoute()
            ErrorPage(
                route = route
            )
        }

        composable<StreamRoute> {
            StreamPage(viewModel = viewModel)
        }
    }
}
