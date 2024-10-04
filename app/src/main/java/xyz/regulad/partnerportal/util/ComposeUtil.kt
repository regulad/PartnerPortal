package xyz.regulad.partnerportal.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Keep the screen on while the composable is active.
 */
@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

/**
 * Show a fullscreen content that hides the system UI (status bar, navigation bar).
 */
@Composable
fun ImmersiveFullscreenContent(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window ?: return // just wait for the activity to be available

    val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(window.decorView.rootWindowInsets)
    val windowInsetsControllerCompat = WindowInsetsControllerCompat(window, window.decorView)

    fun hideUI() {
        if (!windowInsetsCompat.isVisible(WindowInsetsCompat.Type.ime())) { // unreliable, sometimes will return false when the IME is visible
            windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun showUI() {
        windowInsetsControllerCompat.show(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            hideUI()
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        windowInsetsControllerCompat.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        WindowCompat.setDecorFitsSystemWindows(window, false)

        hideUI()

        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            showUI()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }
}

/**
 * Show a toast message. (appears on bottom third of the screen)
 */
fun Context.showToast(message: String) {
    Toast.makeText(this@showToast, message, Toast.LENGTH_LONG).show()
}

/**
 * Navigate to a route, completely clearing the back stack.
 */
fun NavController.navigateOneWay(route: Any) {
    this.navigate(route) {
        popUpTo(this@navigateOneWay.graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

object DialogManager {
    private val looper = Looper.getMainLooper()

    /**
     * Show a dialog with the given title, message, and buttons.
     */
    fun Context.showDialog(
        title: String,
        message: String,
        positiveButtonText: String = "OK",
        negativeButtonText: String? = null,
        onPositiveClick: () -> Unit = {},
        onNegativeClick: () -> Unit = {}
    ) {
        // Ensure we're on the main thread
        Handler(looper).post {
            val builder = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { dialog, _ ->
                    dialog.dismiss()
                    onPositiveClick()
                }

            if (negativeButtonText != null) {
                builder.setNegativeButton(negativeButtonText) { dialog, _ ->
                    dialog.dismiss()
                    onNegativeClick()
                }
            }

            builder.create().show()
        }
    }
}
