package corsica.universita.collecbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import corsica.universita.collecbox.data.model.ItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
abstract class CollecBoxDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: CollecBoxDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): CollecBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CollecBoxDatabase::class.java,
                    "collecbox.db"
                )
                    .addCallback(CollecBoxDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class CollecBoxDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialSeeds(database.itemDao())
                }
            }
        }

        /**
         * Hydratation des 3 graines initiales identiques à la PWA CollecBox
         */
        suspend fun populateInitialSeeds(dao: ItemDao) {
            val seeds = listOf(
                ItemEntity(
                    title = "Random Access Memories",
                    category = "vinyles",
                    barcode = "888837168618",
                    details = "Édition double vinyle 180g (Daft Punk, 2013)",
                    createdAt = System.currentTimeMillis() - 86400000L * 2,
                    isSynced = true
                ),
                ItemEntity(
                    title = "One Piece - Tome 100",
                    category = "mangas",
                    barcode = "9782344049020",
                    details = "Édition collector dorée Glénat (Eiichiro Oda)",
                    createdAt = System.currentTimeMillis() - 86400000L,
                    isSynced = true
                ),
                ItemEntity(
                    title = "The Legend of Zelda: Tears of the Kingdom",
                    category = "jeux",
                    barcode = "0045496478957",
                    details = "Nintendo Switch (Édition physique avec boîtier)",
                    createdAt = System.currentTimeMillis(),
                    isSynced = true
                )
            )
            dao.insertAll(seeds)
        }
    }
}

