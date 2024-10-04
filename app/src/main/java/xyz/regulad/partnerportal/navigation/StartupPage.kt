package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.serialization.Serializable
import xyz.regulad.blueheaven.util.launchAppInfoSettings
import xyz.regulad.partnerportal.MainActivity
import xyz.regulad.partnerportal.PartnerPortalViewModel
import xyz.regulad.partnerportal.ui.minecraft.*
import xyz.regulad.partnerportal.util.DialogManager.showDialog
import xyz.regulad.partnerportal.util.showToast

val VIDEO_CALL_PERMISSIONS = listOf(
    android.Manifest.permission.CAMERA,
    android.Manifest.permission.RECORD_AUDIO
)

@Serializable
data object StartupRoute

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartupPage(viewModel: PartnerPortalViewModel) {
    val activity = (LocalContext.current as? MainActivity)!!

    MinecraftBackgroundImage("dirt.png")

    val intent = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        if (viewModel.connectionJob != null) {
            // we came back here from the StreamPage, so we should restart the activity to ensure that the connection behaves as expected
            restartActivity(intent, lifecycleOwner)
        }
    }

    val permissionState =
        rememberMultiplePermissionsState(
            permissions = VIDEO_CALL_PERMISSIONS,
            onPermissionsResult = { permissions ->
                if (permissions.all { it.value }) {
                    viewModel.startConnection()
                }
            }
        )

    var supabaseUrl by remember { mutableStateOf(viewModel.preferences.supabaseUrl) }
    var supabaseAnonKey by remember { mutableStateOf(viewModel.preferences.supabaseAnonKey) }
    var roomCode by remember { mutableStateOf(viewModel.preferences.roomCode) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            "See GitHub README.md to setup a custom server,\nor just use the default.",
                            fontSize = 12.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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
                            modifier = Modifier.width(400.dp)
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
                            modifier = Modifier.width(400.dp)
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
                            modifier = Modifier.width(400.dp)
                        )
                    }

                    Column {
                        MinecraftButton("Connect") {
                            viewModel.preferences.supabaseUrl = supabaseUrl
                            viewModel.preferences.supabaseAnonKey = supabaseAnonKey
                            viewModel.preferences.roomCode = roomCode

                            if (roomCode.isEmpty()) {
                                activity.showToast("Room code cannot be empty. Choose one! Try to be unique.")
                                return@MinecraftButton
                            }

                            if (supabaseUrl.isEmpty()) {
                                activity.showToast("Supabase URL cannot be empty. Please enter a valid URL.")
                                return@MinecraftButton
                            }

                            if (supabaseAnonKey.isEmpty()) {
                                activity.showToast("Supabase Anon Key cannot be empty. Please enter a valid key.")
                                return@MinecraftButton
                            }

                            if (permissionState.allPermissionsGranted) {
                                viewModel.startConnection()
                                return@MinecraftButton
                            }

                            // we don't have camera/mic permissions, ask for them

                            if (permissionState.shouldShowRationale) {
                                activity.showDialog(
                                    title = "Partner Portal",
                                    message = "Please go to settings and grant the required permissions to continue. If you wouldn't like to do this, you can close the app.",
                                    positiveButtonText = "Go",
                                    onPositiveClick = {
                                        // go to the settings
                                        activity.launchAppInfoSettings()
                                    },
                                    negativeButtonText = "Close App",
                                    onNegativeClick = {
                                        activity.finish()
                                    }
                                )
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
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
