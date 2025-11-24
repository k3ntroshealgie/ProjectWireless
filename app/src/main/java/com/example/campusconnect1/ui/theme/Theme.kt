package com.example.campusconnect1.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Skema Warna Gelap
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = NeoSecondary,
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = DarkBackground,
    onSecondary = DarkText,
    onBackground = DarkText,
    onSurface = DarkText
)

// Skema Warna Terang
private val LightColorScheme = lightColorScheme(
    primary = NeoPrimary,
    secondary = NeoSecondary,
    background = NeoBackground,
    surface = NeoCard,
    onPrimary = NeoCard,
    onSecondary = NeoText,
    onBackground = NeoText,
    onSurface = NeoText
)

@Composable
fun CampusConnect1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Otomatis cek settingan HP
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Update Warna Status Bar (Jam & Baterai)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Icon status bar putih jika dark mode, hitam jika light mode
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}