package corsica.universita.collecbox.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import corsica.universita.collecbox.MainActivity
import corsica.universita.collecbox.R

object NotificationHelper {

    const val CHANNEL_SYNC_ID = "collecbox_sync_channel"
    private const val NOTIFICATION_SYNC_ID = 1001

    /**
     * Création du canal de notification (Requis depuis Android 8.0 / API 26)
     */
    fun createNotificationChannels(context: Context) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_SYNC_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.channel_sync_name))
            .setDescription(context.getString(R.string.channel_sync_description))
            .build()

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * Affiche une notification de synchronisation terminée avec PendingIntent vers MainActivity.
     */
    fun showSyncSuccessNotification(context: Context, count: Int, itemId: Long? = null) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (itemId != null) {
                putExtra("itemId", itemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title_sync))
            .setContentText("$count objet(s) ajouté(s) hors ligne ont été synchronisés !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_SYNC_ID, notification)
    }

    /**
     * Affiche une notification de message push distant FCM
     */
    fun showPushNotification(context: Context, title: String, message: String, itemId: Long? = null) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (itemId != null) {
                putExtra("itemId", itemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            (System.currentTimeMillis() % 10000).toInt(),
            notification
        )
    }
}

