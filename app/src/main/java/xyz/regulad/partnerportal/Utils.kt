package xyz.regulad.partnerportal

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.navigation.NavController

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

fun ByteArray.sha1Hash(): ByteArray {
    val digest = java.security.MessageDigest.getInstance("SHA-1")
    return digest.digest(this)
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
