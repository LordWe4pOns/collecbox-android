package corsica.universita.collecbox.ui.screens.add

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import corsica.universita.collecbox.ui.camera.CameraBarcodeScanner
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

    // Gestion des permissions Android (Slide 13 du cours : Accordé, Rationale, Réglages)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setScanningBarcode(true)
        } else {
            showRationaleDialog = true
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Caméra requise") },
            text = { Text("La caméra est nécessaire pour scanner automatiquement le code-barres (EAN-13, vinyle, manga) via ML Kit.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Autoriser")
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
            title = { Text("Accès caméra bloqué") },
            text = { Text("La permission caméra a été refusée. Vous pouvez l'activer dans les réglages système ou continuer la saisie manuelle.") },
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
                    Text("Continuer manuellement")
                }
            }
        )
    }

    // Affichage plein écran du scanner CameraX + ML Kit dès que le bouton est cliqué
    if (uiState.isScanningBarcode) {
        CameraBarcodeScanner(
            onBarcodeScanned = { scannedBarcode ->
                viewModel.applyBarcodeResult(scannedBarcode)
            },
            onClose = { viewModel.setScanningBarcode(false) }
        )
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

            Spacer(modifier = Modifier.height(16.dp))

            // Champ Code-barres / EAN avec bouton SCANNER TRÈS VISIBLE À DROITE
            OutlinedTextField(
                value = uiState.barcode,
                onValueChange = { viewModel.onBarcodeChange(it) },
                label = { Text("Code-barres / EAN") },
                placeholder = { Text("Scannez ou saisissez l'EAN...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Surface(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(8.dp),
                        color = CollecBoxPrimary,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner le code-barres",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scanner",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CollecBoxSurface,
                    unfocusedContainerColor = CollecBoxSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CollecBoxAccent,
                    unfocusedLabelColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(28.dp))

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
                    .height(52.dp)
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
