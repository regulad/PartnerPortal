package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import xyz.regulad.partnerportal.LoadingRoute
import xyz.regulad.partnerportal.ui.minecraft.HandledAsyncImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftButton
import xyz.regulad.partnerportal.ui.minecraft.MinecraftText

@Composable
fun StartupPage(modifier: Modifier = Modifier, navController: NavController) {
    MinecraftBackgroundImage("dirt.png")

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Column {
                HandledAsyncImage(
                    "splash.png",
                    modifier = Modifier
                        .height(40.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        MinecraftText("Configuration")

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "Supabase Server URL:",
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "Supabase Server Anon Key:",
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "Room Code:",
                            fontSize = 10.sp
                        )
                    }

                    Column {
                        MinecraftButton("Connect") {
                            navController.navigate(LoadingRoute)
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        MinecraftText(
                            "Settings will be saved when\na connection is attempted.",
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "Connect at Startup",
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
