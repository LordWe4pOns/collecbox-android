package corsica.universita.collecbox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import corsica.universita.collecbox.analytics.AnalyticsTracker
import corsica.universita.collecbox.data.model.ItemEntity
import corsica.universita.collecbox.data.preferences.UserPreferencesRepository
import corsica.universita.collecbox.data.repository.CollecBoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<ItemEntity> = emptyList(),
    val selectedCategory: String = "all",
    val unsyncedCount: Int = 0,
    val fcmToken: String? = null,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: CollecBoxRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory

    init {
        viewModelScope.launch {
            preferencesRepository.selectedCategoryFlow.collect { category ->
                _selectedCategory.value = category
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedCategory.flatMapLatest { category ->
            repository.getItemsByCategory(category)
        },
        _selectedCategory,
        preferencesRepository.fcmTokenFlow
    ) { items, category, fcmToken ->
        val unsynced = items.count { !it.isSynced }
        HomeUiState(
            items = items,
            selectedCategory = category,
            unsyncedCount = unsynced,
            fcmToken = fcmToken,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            preferencesRepository.saveSelectedCategory(category)
        }
        // Événement Analytics 2 : Rétention
        analyticsTracker.logCollectionFiltered(category)
    }

    fun triggerSync() {
        repository.scheduleSyncWork()
    }

    class Factory(
        private val repository: CollecBoxRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val analyticsTracker: AnalyticsTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, preferencesRepository, analyticsTracker) as T
        }
    }
}

