package corsica.universita.collecbox.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Gestionnaire d'Analytics pour les 3 événements obligatoires du TP (Slide 7) :
 * 1. Activation : Premier geste utile (ex: item_created)
 * 2. Rétention : Retour de l'utilisateur (ex: collection_filtered, notification_opened)
 * 3. Le natif : Utilisation de la fonctionnalité native (ex: camera_ocr_scanned, widget_refreshed)
 */
class AnalyticsTracker(context: Context) {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    init {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        } catch (e: Exception) {
            Log.w("AnalyticsTracker", "Firebase Analytics non initialisé : journalisation locale")
        }
    }

    /**
     * Événement d'ouverture de session
     */
    fun logAppOpen() {
        val bundle = Bundle().apply {
            putLong("timestamp", System.currentTimeMillis())
        }
        log("app_open", bundle)
    }

    /**
     * Événement 1 : Activation (Premier geste utile - Création d'un objet)
     */
    fun logItemCreated(category: String, isOffline: Boolean, hasOcr: Boolean) {
        val bundle = Bundle().apply {
            putString("category", category)
            putBoolean("offline_created", isOffline)
            putBoolean("used_camera_ocr", hasOcr)
        }
        log("item_created", bundle)
    }

    /**
     * Événement 2 : Rétention (Changement de filtre ou consultation)
     */
    fun logCollectionFiltered(category: String) {
        val bundle = Bundle().apply {
            putString("category_filter", category)
        }
        log("collection_filtered", bundle)
    }

    /**
     * Événement 3 : Le Natif (Scanner Code-barres / EAN CameraX + ML Kit on-device)
     */
    fun logBarcodeScanned(barcode: String) {
        val bundle = Bundle().apply {
            putString("barcode_value", barcode)
            putInt("barcode_length", barcode.length)
        }
        log("barcode_scanned", bundle)
    }

    /**
     * Événement 3 bis : Le Natif (Reconnaissance de texte par caméra ML Kit on-device)
     */
    fun logCameraOcrScanned(detectedWordsCount: Int, extractedVolume: String?) {
        val bundle = Bundle().apply {
            putInt("detected_words_count", detectedWordsCount)
            putString("extracted_volume", extractedVolume ?: "none")
        }
        log("camera_ocr_scanned", bundle)
    }

    /**
     * Événement 3 bis : Le Natif (Interaction Widget Glance)
     */
    fun logWidgetInteraction(action: String) {
        val bundle = Bundle().apply {
            putString("widget_action", action)
        }
        log("widget_interaction", bundle)
    }

    private fun log(name: String, bundle: Bundle) {
        Log.d("AnalyticsTracker", "📊 Event [$name] -> $bundle")
        try {
            firebaseAnalytics?.logEvent(name, bundle)
        } catch (e: Exception) {
            Log.w("AnalyticsTracker", "Erreur d'envoi analytics : ${e.message}")
        }
    }
}

