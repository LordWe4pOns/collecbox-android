package corsica.universita.collecbox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un objet de collection CollecBox.
 * Correspondance directe avec le Store IndexedDB 'items' de la PWA.
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // "vinyles", "mangas", "jeux", "bd"
    val barcode: String = "",
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

