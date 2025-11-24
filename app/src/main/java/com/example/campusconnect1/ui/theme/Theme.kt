package com.example.campusconnect1.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Skema Warna Terang (Neo Academia) - KITA HANYA PAKAI INI
private val LightColorScheme = lightColorScheme(
    primary = NeoPrimary,
    secondary = NeoSecondary,
    background = NeoBackground, // Slate 50
    surface = NeoCard,          // Putih
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = NeoText,     // Teks Hitam
    onSurface = NeoText,        // Teks Hitam
    surfaceVariant = Color(0xFFE2E8F0), // Slate 200
    onSurfaceVariant = NeoTextLight,
    outline = Color.LightGray
)

@Composable
fun CampusConnect1Theme(
    // Kita hapus parameter darkTheme, tidak butuh lagi
    content: @Composable () -> Unit
) {
    // LANGSUNG PAKAI LIGHT SCHEME
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar warna background (terang)
            window.statusBarColor = colorScheme.background.toArgb()
            // Ikon status bar SELALU GELAP (karena background terang)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}