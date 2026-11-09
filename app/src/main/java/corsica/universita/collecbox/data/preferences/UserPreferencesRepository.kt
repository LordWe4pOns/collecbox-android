package corsica.universita.collecbox.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "collecbox_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_CATEGORY = stringPreferencesKey("selected_category")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val selectedCategoryFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_CATEGORY] ?: "all"
        }

    val fcmTokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.FCM_TOKEN]
        }

    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DARK_THEME] ?: true
        }

    suspend fun saveSelectedCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_CATEGORY] = category
        }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FCM_TOKEN] = token
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME] = enabled
        }
    }
}

