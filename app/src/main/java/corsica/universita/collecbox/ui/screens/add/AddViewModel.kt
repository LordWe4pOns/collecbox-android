package corsica.universita.collecbox.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import corsica.universita.collecbox.analytics.AnalyticsTracker
import corsica.universita.collecbox.data.model.ItemEntity
import corsica.universita.collecbox.data.repository.CollecBoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddItemUiState(
    val title: String = "",
    val category: String = "mangas",
    val barcode: String = "",
    val details: String = "",
    val isScanningOcr: Boolean = false,
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val usedOcr: Boolean = false
)

class AddViewModel(
    private val repository: CollecBoxRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onCategoryChange(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onBarcodeChange(barcode: String) {
        _uiState.value = _uiState.value.copy(barcode = barcode)
    }

    fun onDetailsChange(details: String) {
        _uiState.value = _uiState.value.copy(details = details)
    }

    fun setScanningOcr(scanning: Boolean) {
        _uiState.value = _uiState.value.copy(isScanningOcr = scanning)
    }

    fun applyOcrResult(extractedTitle: String, extractedDetails: String) {
        _uiState.value = _uiState.value.copy(
            title = extractedTitle,
            details = extractedDetails,
            isScanningOcr = false,
            usedOcr = true
        )
        // Événement Analytics 3 : Le Natif (Camera OCR ML Kit)
        analyticsTracker.logCameraOcrScanned(
            detectedWordsCount = extractedTitle.split(" ").size,
            extractedVolume = extractedDetails
        )
    }

    fun saveItem(onSuccess: (Long) -> Unit) {
        val current = _uiState.value
        if (current.title.isBlank()) return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            val item = ItemEntity(
                title = current.title.trim(),
                category = current.category,
                barcode = current.barcode.trim(),
                details = current.details.trim(),
                createdAt = System.currentTimeMillis(),
                isSynced = false
            )

            val id = repository.addItem(item)

            // Événement Analytics 1 : Activation (Premier geste utile)
            analyticsTracker.logItemCreated(
                category = item.category,
                isOffline = true, // Sauvegardé en Outbox locale
                hasOcr = current.usedOcr
            )

            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            onSuccess(id)
        }
    }

    class Factory(
        private val repository: CollecBoxRepository,
        private val analyticsTracker: AnalyticsTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddViewModel(repository, analyticsTracker) as T
        }
    }
}

