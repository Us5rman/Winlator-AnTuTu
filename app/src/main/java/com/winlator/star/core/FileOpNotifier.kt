package com.winlator.star.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.winlator.star.R

/**
 * Posts a single, reused, updating notification tracking the progress of a copy, move,
 * compress, or extract operation in the file manager — mirroring the in-app progress dialog.
 *
 * NOTE: uses the launcher icon as the small icon since no dedicated monochrome notification
 * icon exists in this project yet. Android will render/tint a full-color icon awkwardly on
 * some versions; swap in a proper white-silhouette drawable under res/drawable if you add one.
 */
object FileOpNotifier {
    private const val CHANNEL_ID = "file_operations"
    private const val CHANNEL_NAME = "File Operations"
    private const val NOTIFICATION_ID = 4201

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW // progress only — no sound/heads-up popup
                ).apply {
                    description = "Progress for copy, move, compress, and extract operations."
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun hasPermission(context: Context): Boolean {
        // POST_NOTIFICATIONS is only required/checked on Android 13+; older versions
        // don't gate notification posting behind a runtime permission at all.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    /** Shows or updates the ongoing progress notification. Safe to call frequently. */
    fun update(context: Context, title: String, itemName: String, progressPercent: Int, indeterminate: Boolean) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(if (indeterminate) itemName else "$itemName — $progressPercent%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progressPercent, indeterminate)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    /** Dismisses the progress notification — call on success, failure, and cancellation alike. */
    fun clear(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
