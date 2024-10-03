package xyz.regulad.blueheaven.util

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper

object DialogManager {
    private val looper = Looper.getMainLooper()

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
