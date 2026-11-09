package corsica.universita.collecbox.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import corsica.universita.collecbox.ui.theme.CollecBoxAccent
import corsica.universita.collecbox.ui.theme.CollecBoxPrimary
import java.util.concurrent.Executors

/**
 * Fonctionnalité Native 2 : CameraX + ML Kit Text Recognition on-device (OCR).
 * Analyse en temps réel les tranches de livres, pochettes de vinyles ou boîtiers de jeux.
 * 100% hors-ligne dans le process Android, impossible avec cette réactivité en PWA.
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreviewWithOcr(
    onTextExtracted: (title: String, details: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedTextSummary by remember { mutableStateOf("Pointez la caméra vers un titre ou une tranche...") }
    var detectedTitle by remember { mutableStateOf("") }
    var detectedDetails by remember { mutableStateOf("") }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            textRecognizer.close()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            textRecognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    val blocks = visionText.textBlocks
                                    if (blocks.isNotEmpty()) {
                                        val fullText = visionText.text.trim()
                                        val lines = fullText.split("\n").filter { it.isNotBlank() }

                                        val possibleTitle = lines.firstOrNull() ?: ""

                                        // Regex de détection de numéro de tome/volume (ex: "Tome 5", "Vol. 12", "#03")
                                        val volumeRegex = Regex("""(?i)(tome|vol\.?|volume|#)\s*(\d+)""")
                                        val volumeMatch = volumeRegex.find(fullText)?.value

                                        detectedTitle = possibleTitle
                                        detectedDetails = if (volumeMatch != null) {
                                            "Détecté par OCR : $volumeMatch"
                                        } else {
                                            "Texte détecté : ${lines.take(2).joinToString(" - ")}"
                                        }

                                        detectedTextSummary = "📖 $possibleTitle ${volumeMatch?.let { "($it)" } ?: ""}"
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("CameraOcr", "Erreur OCR : ${e.message}")
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraOcr", "Échec d'initialisation caméra", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay d'assistance visuelle
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC0F172A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Scanner IA temps réel (ML Kit)",
                        color = CollecBoxAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detectedTextSummary,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (detectedTitle.isNotBlank()) {
                                onTextExtracted(detectedTitle, detectedDetails)
                            }
                        },
                        enabled = detectedTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CollecBoxPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✨ Remplir la fiche avec ce texte")
                    }
                }
            }
        }
    }
}

