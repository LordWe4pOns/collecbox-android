package corsica.universita.collecbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CollecBoxPrimary,
    onPrimary = Color.White,
    secondary = CollecBoxAccent,
    onSecondary = Color.White,
    background = CollecBoxDarkBg,
    onBackground = CollecBoxTextPrimary,
    surface = CollecBoxSurface,
    onSurface = CollecBoxTextPrimary,
    surfaceVariant = CollecBoxCard,
    onSurfaceVariant = CollecBoxTextSecondary
)

@Composable
fun CollecBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

