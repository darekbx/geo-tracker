package com.darekbx.geotracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityTransitionResult

class ActivityTranstionReceiver : BroadcastReceiver() {

    companion object {
        @JvmStatic
        val ACTION = "newAction"

        const val NOTIFICATION_ID = 202
        const val NOTIFICATION_CHANNEL_ID = "activity_channel_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            if (result.transitionEvents.isNotEmpty()) {
                showNotification(
                    context,
                    "Activity: ${result.transitionEvents.first().activityType}",
                    "Transition: ${result.transitionEvents.first().transitionType}"
                )
            }
        }
    }

    private fun showNotification(context: Context, title: String, text: String) {
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)

        if (channel == null) {
            channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, title, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}