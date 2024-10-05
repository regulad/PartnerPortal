package xyz.regulad.partnerportal.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.serialization.Serializable
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftButton
import xyz.regulad.partnerportal.ui.minecraft.MinecraftText

@Serializable
data class ErrorRoute(
    val errorMessage: String
)

fun restartActivity(context: Context, lifecycleOwner: LifecycleOwner) {
    val intent = (context as? Activity)?.intent ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    // Finish the current activity
    (context as? Activity)?.finish()

    // Clear ViewModels
    (lifecycleOwner as? ComponentActivity)?.viewModelStore?.clear()

    // Start the activity again, after some delay
    context.startActivity(intent)
}

@Composable
fun ErrorPage(route: ErrorRoute) {
    MinecraftBackgroundImage("dirt.png")

    val intent = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
                // kill the activity
                restartActivity(intent, lifecycleOwner)
            }
        }
    }
}
