package corsica.universita.collecbox.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import corsica.universita.collecbox.CollecBoxApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service FCM pour la réception de messages distants même quand l'app est tuée.
 * Requis par la section Push Notification du TP.
 */
class CollecBoxMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "CollecBoxFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nouveau jeton FCM : $token")
        // Sauvegarde dans Jetpack DataStore pour affichage / copie dans l'UI
        val app = applicationContext as? CollecBoxApplication ?: return
        serviceScope.launch {
            app.userPreferencesRepository.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message reçu de : ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "CollecBox"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Nouvelle alerte de collection"

        val itemId = remoteMessage.data["itemId"]?.toLongOrNull()

        NotificationHelper.showPushNotification(applicationContext, title, body, itemId)
    }
}

