package corsica.universita.collecbox

import android.app.Application
import corsica.universita.collecbox.analytics.AnalyticsTracker
import corsica.universita.collecbox.data.local.CollecBoxDatabase
import corsica.universita.collecbox.data.preferences.UserPreferencesRepository
import corsica.universita.collecbox.data.repository.CollecBoxRepository
import corsica.universita.collecbox.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application class pour l'initialisation du graphe de dépendances léger,
 * des canaux de notification, et du stockage persistant.
 */
class CollecBoxApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { CollecBoxDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { CollecBoxRepository(database.itemDao(), this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val analyticsTracker by lazy { AnalyticsTracker(this) }

    override fun onCreate() {
        super.onCreate()
        // Création des canaux de notification au démarrage (Android 8.0+)
        NotificationHelper.createNotificationChannels(this)
    }
}

