package corsica.universita.collecbox.ui.screens.add

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import corsica.universita.collecbox.ui.camera.CameraPreviewWithOcr
import corsica.universita.collecbox.ui.theme.CollecBoxAccent
import corsica.universita.collecbox.ui.theme.CollecBoxDarkBg
import corsica.universita.collecbox.ui.theme.CollecBoxPrimary
import corsica.universita.collecbox.ui.theme.CollecBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddViewModel,
    onNavigateBack: () -> Unit,
    onItemAdded: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Gestion du cycle de permissions Android (Slide 13 du TP : Accordé, Rationale, Réglages)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setScanningOcr(true)
        } else {
            showRationaleDialog = true
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Caméra nécessaire") },
            text = { Text("La caméra est nécessaire pour scanner automatiquement la tranche des mangas/BD et les pochettes via le moteur ML Kit hors-ligne.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Réessayer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    showSettingsDialog = true
                }) {
                    Text("Ouvrir les réglages")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Accès bloqué") },
            text = { Text("Vous avez refusé la permission caméra. Veuillez l'activer manuellement dans les paramètres de l'application.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Aller aux réglages")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Affichage plein écran de la caméra OCR
    if (uiState.isScanningOcr) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreviewWithOcr(
                onTextExtracted = { title, details ->
                    viewModel.applyOcrResult(title, details)
                }
            )
            IconButton(
                onClick = { viewModel.setScanningOcr(false) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color(0x99000000), shape = RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
            }
        }
        return
    }

    val categories = listOf(
        "mangas" to "📖 Mangas",
        "vinyles" to "💿 Vinyles",
        "jeux" to "🎮 Jeux",
        "bd" to "🎨 BD"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un objet", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CollecBoxSurface)
            )
        },
        containerColor = CollecBoxDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Bouton vers la fonctionnalité native : Caméra OCR ML Kit
            Card(
                colors = CardDefaults.cardColors(containerColor = CollecBoxSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Reconnaissance de texte (ML Kit)",
                            color = CollecBoxAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Scannez la tranche ou le titre avec la caméra en temps réel",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = CollecBoxPrimary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanner")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Catégorie",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { (key, label) ->
                    val isSelected = uiState.category == key
                    AssistChip(
                        onClick = { viewModel.onCategoryChange(key) },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Champ Titre
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Titre de l'œuvre *") },
                placeholder = { Text("Ex: One Piece - Tome 101") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CollecBoxSurface,
                    unfocusedContainerColor = CollecBoxSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CollecBoxAccent,
                    unfocusedLabelColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Champ Code-barres
            OutlinedTextField(
                value = uiState.barcode,
                onValueChange = { viewModel.onBarcodeChange(it) },
                label = { Text("Code-barres / EAN (optionnel)") },
                placeholder = { Text("Ex: 9782344049020") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CollecBoxSurface,
                    unfocusedContainerColor = CollecBoxSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CollecBoxAccent,
                    unfocusedLabelColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Champ Détails / Description
            OutlinedTextField(
                value = uiState.details,
                onValueChange = { viewModel.onDetailsChange(it) },
                label = { Text("Détails / Édition / Notes") },
                placeholder = { Text("Ex: Première édition, état neuf") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CollecBoxSurface,
                    unfocusedContainerColor = CollecBoxSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CollecBoxAccent,
                    unfocusedLabelColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton de validation
            Button(
                onClick = {
                    viewModel.saveItem { newId ->
                        onItemAdded(newId)
                    }
                },
                enabled = uiState.title.isNotBlank() && !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CollecBoxPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isSaving) "Enregistrement..." else "Ajouter à ma collection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

