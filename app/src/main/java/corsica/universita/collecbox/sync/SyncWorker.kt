package corsica.universita.collecbox.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import corsica.universita.collecbox.CollecBoxApplication
import corsica.universita.collecbox.notification.NotificationHelper

/**
 * Worker WorkManager chargé de vider l'Outbox (éléments non synchronisés).
 * Remplacement natif robuste de Background Sync de la PWA.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "collecbox_outbox_sync_work"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as CollecBoxApplication
        return try {
            val syncedCount = app.repository.flushOutbox()
            if (syncedCount > 0) {
                NotificationHelper.showSyncSuccessNotification(applicationContext, syncedCount)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

