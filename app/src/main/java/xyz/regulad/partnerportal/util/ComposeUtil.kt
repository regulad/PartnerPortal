package xyz.regulad.partnerportal.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
  import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
        windowInsetsControllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

fun Context.showToast(message: String) {
    Toast.makeText(this@showToast, message, Toast.LENGTH_LONG).show()
}
