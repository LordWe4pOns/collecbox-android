package corsica.universita.collecbox.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import corsica.universita.collecbox.data.model.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) pour les objets de collection.
 * Utilise Flow pour diffuser les mises à jour réactives à l'UI sans polling.
 */
@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedItems(): List<ItemEntity>

    @Query("SELECT COUNT(*) FROM items")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE category = :category")
    fun countByCategory(category: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>): List<Long>

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

