package by.grinich.hscontrol.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ============================================================================
// НОВАЯ ПАЛИТРА — Deep Purple + Honor Red акцент
// ============================================================================

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF341B70),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = Color(0xFFFF375F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF5C0019),
    onSecondaryContainer = Color(0xFFFFD9DE),

    tertiary = Color(0xFF00D9B5),
    onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF005144),
    onTertiaryContainer = Color(0xFFB2FFE6),

    background = Color(0xFF0E0E12),
    onBackground = Color(0xFFE5E1E6),
    surface = Color(0xFF1A1A22),
    onSurface = Color(0xFFE5E1E6),
    surfaceVariant = Color(0xFF252530),
    onSurfaceVariant = Color(0xFFB0AEC0),

    error = Color(0xFFFF5449),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF5C1A15),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF3A3A48),
    outlineVariant = Color(0xFF2A2A36),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF22005D),

    secondary = Color(0xFFE91E63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF400014),

    tertiary = Color(0xFF00A88F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2FFE6),
    onTertiaryContainer = Color(0xFF002117),

    background = Color(0xFFF7F5FB),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0EDF7),
    onSurfaceVariant = Color(0xFF48454E),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE7E0EC),
)

// ============================================================================
// СОВРЕМЕННЫЕ ФОРМЫ — крупные скругления как в iOS/Material3
// ============================================================================

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ============================================================================
// ТИПОГРАФИКА — мягкие веса
// ============================================================================

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
)

// ============================================================================
// СОХРАНЕНИЕ ТЕМЫ
// ============================================================================

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("hs_control_prefs", Context.MODE_PRIVATE)

    fun getDarkMode(): Boolean? {
        if (!prefs.contains("dark_mode")) return null
        return prefs.getBoolean("dark_mode", false)
    }

    fun setDarkMode(value: Boolean) {
        prefs.edit().putBoolean("dark_mode", value).apply()
    }

    fun clearDarkMode() {
        prefs.edit().remove("dark_mode").apply()
    }
}

// ============================================================================
// ГЛАВНАЯ ФУНКЦИЯ ТЕМЫ
// ============================================================================

@Composable
fun HSControlTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = darkTheme ?: systemDark
    val colors = if (isDark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}