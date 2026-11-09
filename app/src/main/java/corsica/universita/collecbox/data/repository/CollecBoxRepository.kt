package corsica.universita.collecbox.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import corsica.universita.collecbox.data.local.ItemDao
import corsica.universita.collecbox.data.model.ItemEntity
import corsica.universita.collecbox.data.remote.RetrofitClient
import corsica.universita.collecbox.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository unifié : source unique de vérité pour l'application.
 * Architecture hors ligne par construction (Local-First) :
 * - Les écritures se font d'abord dans Room SQLite.
 * - Un travail WorkManager avec contrainte réseau est programmé pour vider l'Outbox.
 */
class CollecBoxRepository(
    private val itemDao: ItemDao,
    private val context: Context
) {

    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()

    fun getItemsByCategory(category: String): Flow<List<ItemEntity>> {
        return if (category == "all") {
            itemDao.getAllItems()
        } else {
            itemDao.getItemsByCategory(category)
        }
    }

    fun getItemById(id: Long): Flow<ItemEntity?> = itemDao.getItemById(id)

    fun getTotalCount(): Flow<Int> = itemDao.count()

    /**
     * Ajout Local-First :
     * 1. Insertion locale immédiate dans Room avec isSynced = false
     * 2. Déclenchement de WorkManager pour synchroniser dès que le réseau est disponible
     */
    suspend fun addItem(item: ItemEntity): Long = withContext(Dispatchers.IO) {
        val newId = itemDao.insert(item.copy(isSynced = false))
        scheduleSyncWork()
        newId
    }

    suspend fun deleteItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        itemDao.delete(item)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        itemDao.deleteById(id)
    }

    /**
     * Programmation d'un travail unique WorkManager avec contrainte CONNECTED.
     * Garantit la synchronisation différée même après fermeture de l'app ou redémarrage de l'OS.
     */
    fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Exécution directe de la synchronisation (appelée par SyncWorker sur Dispatchers.IO)
     */
    suspend fun flushOutbox(): Int = withContext(Dispatchers.IO) {
        val unsyncedItems = itemDao.getUnsyncedItems()
        if (unsyncedItems.isEmpty()) return@withContext 0

        var syncedCount = 0
        val syncedIds = mutableListOf<Long>()

        for (item in unsyncedItems) {
            try {
                val response = RetrofitClient.apiService.syncItem(item)
                if (response.isSuccessful) {
                    syncedIds.add(item.id)
                    syncedCount++
                }
            } catch (e: Exception) {
                // En cas d'échec réseau, l'item reste isSynced = false pour la tentative suivante
            }
        }

        if (syncedIds.isNotEmpty()) {
            itemDao.markAsSynced(syncedIds)
        }

        syncedCount
    }
}

