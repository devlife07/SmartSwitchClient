package com.vt.smart.switchapp.utils.utilities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ui.activity.Splash
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessaging : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val params = remoteMessage.data
        if (params.isNotEmpty()) {
            sendNotification(params["title"], params["message"])
            broadcastNewNotification()
        } else {
            sendNotification(
                remoteMessage.notification!!.title, remoteMessage.notification!!.body
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, Splash::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("Push Notification", title)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                resources.getString(R.string.app_name),
                resources.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH
            )
            mChannel.enableLights(true)
            mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(mChannel)
        }
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, resources.getString(R.string.app_name))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_splash)
                .setColor(resources.getColor(R.color.black))
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setContentIntent(pendingIntent)
                .setChannelId(resources.getString(R.string.app_name))
                .setFullScreenIntent(pendingIntent, true)
        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun broadcastNewNotification() {
        val intent = Intent("new_notification")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
