package xyz.regulad.partnerportal

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import xyz.regulad.partnerportal.ui.theme.PartnerPortalTheme
import xyz.regulad.partnerportal.util.ImmersiveFullscreenContent
import xyz.regulad.partnerportal.util.KeepScreenOn

class MainActivity : ComponentActivity() {
    private val viewModel: PartnerPortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // force landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeepScreenOn()

            val navController = rememberNavController()

            PartnerPortalTheme {
                ImmersiveFullscreenContent {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> // we don't use the padding here
                        PartnerPortalNavHost(
                            navHostController = navController
                        )
                    }
                }
            }
        }
    }
}
