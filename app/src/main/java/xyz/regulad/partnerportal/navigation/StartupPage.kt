package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import xyz.regulad.partnerportal.LoadingRoute
import xyz.regulad.partnerportal.PartnerPortalViewModel
import xyz.regulad.partnerportal.ui.minecraft.*

@Composable
fun StartupPage(modifier: Modifier = Modifier, viewModel: PartnerPortalViewModel, navController: NavController) {
    MinecraftBackgroundImage("dirt.png")

    var supabaseUrl by remember { mutableStateOf(viewModel.preferences.supabaseUrl) }
    var supabaseAnonKey by remember { mutableStateOf(viewModel.preferences.supabaseAnonKey) }
    var roomCode by remember { mutableStateOf(viewModel.preferences.roomCode) }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        MinecraftText(
                            "Supabase Server URL:",
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(5.dp))


                        MinecraftTextField(
                            supabaseUrl,
                            onValueChange = {
                                supabaseUrl = it
                            },
                            modifier = Modifier.width(350.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "Supabase Server Anon Key:",
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        MinecraftTextField(
                            supabaseAnonKey,
                            onValueChange = {
                                supabaseAnonKey = it
                            },
                            modifier = Modifier.width(350.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "Room Code:",
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        MinecraftTextField(
                            roomCode,
                            onValueChange = {
                                roomCode = it
                            },
                            modifier = Modifier.width(350.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText(
                            "See GitHub README.md on setting up a Supabase server.",
                            fontSize = 10.sp
                        )
                    }

                    Column {
                        MinecraftButton("Connect") {
                            viewModel.preferences.supabaseUrl = supabaseUrl
                            viewModel.preferences.supabaseAnonKey = supabaseAnonKey
                            viewModel.preferences.roomCode = roomCode
                            
                            // save done, now navigate
                            navController.navigate(LoadingRoute)
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        MinecraftText(
                            "Settings will be saved when\na connection is attempted.",
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
