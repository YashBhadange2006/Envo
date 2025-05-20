package com.example.envo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

val nightColorScheme = darkColorScheme(
    primary = NightGreen,
    onPrimary = NightText,
    background = NightBlue,
    onBackground = NightText,
    surface = Color(0xFF16222E),
    onSurface = Color(0xFFE0E0E0),
    secondary = Color(0xFF4F5B62),
    onSecondary = NightText,
    tertiary = Color(0xFFB0BEC5),
    onTertiary = NightText
)

val Context.dataStore by preferencesDataStore(name = "settings")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

suspend fun setDarkMode(context: Context, enabled: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[DARK_MODE_KEY] = enabled
    }
}

fun getDarkModeFlow(context: Context): Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[DARK_MODE_KEY] ?: false }

@Composable
fun rememberDarkModeState(): Pair<Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkModeState = getDarkModeFlow(context).collectAsState(initial = false)
    val darkMode = darkModeState.value
    val setDarkModeState: (Boolean) -> Unit = { enabled ->
        scope.launch { setDarkMode(context, enabled) }
    }
    return darkMode to setDarkModeState
}

@Composable
fun EnvoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> nightColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}