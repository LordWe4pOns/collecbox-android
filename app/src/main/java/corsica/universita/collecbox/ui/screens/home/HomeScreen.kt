package corsica.universita.collecbox.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import corsica.universita.collecbox.data.model.ItemEntity
import corsica.universita.collecbox.ui.theme.CollecBoxAccent
import corsica.universita.collecbox.ui.theme.CollecBoxDarkBg
import corsica.universita.collecbox.ui.theme.CollecBoxPrimary
import corsica.universita.collecbox.ui.theme.CollecBoxSuccess
import corsica.universita.collecbox.ui.theme.CollecBoxSurface
import corsica.universita.collecbox.ui.theme.CollecBoxWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val categories = listOf(
        "all" to "Tous",
        "vinyles" to "💿 Vinyles",
        "mangas" to "📖 Mangas",
        "jeux" to "🎮 Jeux",
        "bd" to "🎨 BD"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "CollecBox",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Native",
                            fontSize = 12.sp,
                            color = CollecBoxAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    // Bouton pour copier le jeton FCM pour les tests du TP
                    IconButton(onClick = {
                        val token = uiState.fcmToken
                        if (!token.isNullOrEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", token))
                            Toast.makeText(context, "Jeton FCM copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                        } else {
                            onRequestNotificationPermission()
                            Toast.makeText(context, "Demande de permission de notification...", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications FCM",
                            tint = Color.White
                        )
                    }

                    // Bouton de déclenchement forcé de synchronisation
                    IconButton(onClick = { viewModel.triggerSync() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchroniser maintenant",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CollecBoxSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = CollecBoxPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un objet")
            }
        },
        containerColor = CollecBoxDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Bandeau de synchronisation hors-ligne (Outbox)
            if (uiState.unsyncedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CollecBoxWarning.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = CollecBoxWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.unsyncedCount} en attente de synchro",
                            color = CollecBoxWarning,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "WorkManager actif",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Filtres par catégorie
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (key, label) ->
                    val isSelected = uiState.selectedCategory == key
                    AssistChip(
                        onClick = { viewModel.selectCategory(key) },
                        label = {
                            Text(
                                label,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) CollecBoxPrimary else CollecBoxSurface
                        ),
                        border = null
                    )
                }
            }

            // Liste des objets
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun objet dans cette catégorie.\nAppuyez sur + pour scanner ou ajouter.",
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ItemCard(item = item, onClick = { onNavigateToDetail(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: ItemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CollecBoxSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryIcon = when (item.category.lowercase()) {
                        "vinyles" -> "💿"
                        "mangas" -> "📖"
                        "jeux" -> "🎮"
                        "bd" -> "🎨"
                        else -> "📦"
                    }
                    Text(
                        text = "$categoryIcon ${item.category.uppercase()}",
                        color = CollecBoxAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                if (item.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.details,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            // Statut de synchronisation Outbox
            Box(modifier = Modifier.padding(start = 12.dp)) {
                if (item.isSynced) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Synchronisé",
                        tint = CollecBoxSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = "En attente de synchro",
                        tint = CollecBoxWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

