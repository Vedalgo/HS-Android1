package by.grinich.hscontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import by.grinich.hscontrol.ui.theme.HSControlTheme
import by.grinich.hscontrol.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            var darkMode by remember { mutableStateOf(themePrefs.getDarkMode()) }

            HSControlTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        darkMode = darkMode,
                        onDarkModeChange = { newValue ->
                            darkMode = newValue
                            if (newValue == null) {
                                themePrefs.clearDarkMode()
                            } else {
                                themePrefs.setDarkMode(newValue)
                            }
                        }
                    )
                }
            }
        }
    }
}