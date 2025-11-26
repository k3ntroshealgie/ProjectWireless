package com.example.campusconnect1.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// MODERN DESIGN SYSTEM - Material Design 3
// Inspired by LinkedIn & Reddit (2024-2025)
// ============================================

// --- LIGHT MODE COLORS ---
// Primary: Professional Blue (LinkedIn-inspired)
val LightPrimary = Color(0xFF0A66C2)           // LinkedIn Blue
val LightOnPrimary = Color(0xFFFFFFFF)         // White text on primary
val LightPrimaryContainer = Color(0xFFD6E4FF)  // Light blue container
val LightOnPrimaryContainer = Color(0xFF001C3A) // Dark blue text

// Secondary: Slate Gray
val LightSecondary = Color(0xFF64748B)         // Slate 500
val LightOnSecondary = Color(0xFFFFFFFF)       // White text on secondary
val LightSecondaryContainer = Color(0xFFE2E8F0) // Slate 200
val LightOnSecondaryContainer = Color(0xFF1E293B) // Slate 800

// Tertiary: Emerald Green (Success/Accent)
val LightTertiary = Color(0xFF10B981)          // Emerald 500
val LightOnTertiary = Color(0xFFFFFFFF)        // White text
val LightTertiaryContainer = Color(0xFFD1FAE5) // Emerald 100
val LightOnTertiaryContainer = Color(0xFF065F46) // Emerald 800

// Background & Surface
val LightBackground = Color(0xFFF3F4F6)        // Gray 100
val LightOnBackground = Color(0xFF111827)      // Gray 900
val LightSurface = Color(0xFFFFFFFF)           // Pure White
val LightOnSurface = Color(0xFF111827)         // Gray 900
val LightSurfaceVariant = Color(0xFFF9FAFB)    // Gray 50
val LightOnSurfaceVariant = Color(0xFF6B7280)  // Gray 500

// Outline & Borders
val LightOutline = Color(0xFFD1D5DB)           // Gray 300
val LightOutlineVariant = Color(0xFFE5E7EB)    // Gray 200

// Error
val LightError = Color(0xFFEF4444)             // Red 500
val LightOnError = Color(0xFFFFFFFF)           // White
val LightErrorContainer = Color(0xFFFEE2E2)    // Red 100
val LightOnErrorContainer = Color(0xFF991B1B)  // Red 800

// --- DARK MODE COLORS ---
// Primary: Lighter Blue for dark backgrounds
val DarkPrimary = Color(0xFF5B9FED)            // Lighter LinkedIn Blue
val DarkOnPrimary = Color(0xFF003258)          // Dark blue
val DarkPrimaryContainer = Color(0xFF004A77)   // Medium blue
val DarkOnPrimaryContainer = Color(0xFFD6E4FF) // Light blue

// Secondary: Lighter Slate
val DarkSecondary = Color(0xFF94A3B8)          // Slate 400
val DarkOnSecondary = Color(0xFF1E293B)        // Slate 800
val DarkSecondaryContainer = Color(0xFF334155) // Slate 700
val DarkOnSecondaryContainer = Color(0xFFE2E8F0) // Slate 200

// Tertiary: Lighter Emerald
val DarkTertiary = Color(0xFF34D399)           // Emerald 400
val DarkOnTertiary = Color(0xFF065F46)         // Emerald 800
val DarkTertiaryContainer = Color(0xFF047857)  // Emerald 700
val DarkOnTertiaryContainer = Color(0xFFD1FAE5) // Emerald 100

// Background & Surface
val DarkBackground = Color(0xFF0F172A)         // Slate 900
val DarkOnBackground = Color(0xFFF1F5F9)       // Slate 100
val DarkSurface = Color(0xFF1E293B)            // Slate 800
val DarkOnSurface = Color(0xFFF1F5F9)          // Slate 100
val DarkSurfaceVariant = Color(0xFF334155)     // Slate 700
val DarkOnSurfaceVariant = Color(0xFFCBD5E1)   // Slate 300

// Outline & Borders
val DarkOutline = Color(0xFF475569)            // Slate 600
val DarkOutlineVariant = Color(0xFF334155)     // Slate 700

// Error
val DarkError = Color(0xFFF87171)              // Red 400
val DarkOnError = Color(0xFF7F1D1D)            // Red 900
val DarkErrorContainer = Color(0xFF991B1B)     // Red 800
val DarkOnErrorContainer = Color(0xFFFEE2E2)   // Red 100

// --- SEMANTIC COLORS (Theme-independent) ---
val SuccessGreen = Color(0xFF10B981)           // Emerald 500
val WarningOrange = Color(0xFFF59E0B)          // Amber 500
val InfoBlue = Color(0xFF3B82F6)               // Blue 500

// --- LEGACY COLORS (Keep for backward compatibility during migration) ---
val NeoPrimary = LightPrimary
val NeoSecondary = LightSecondary
val NeoAccent = LightTertiary
val NeoBackground = LightBackground
val NeoCard = LightSurface
val NeoText = LightOnSurface
val NeoTextLight = LightOnSurfaceVariant