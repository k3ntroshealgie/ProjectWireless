package com.example.campusconnect1.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================
// SHAPE SYSTEM - Material Design 3
// Defines corner radius for different components
// ============================================

val AppShapes = Shapes(
    // Small components: Chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    
    // Medium components: Cards, standard buttons, text fields
    medium = RoundedCornerShape(12.dp),
    
    // Large components: Dialogs, large cards
    large = RoundedCornerShape(16.dp),
    
    // Extra large: Bottom sheets, modals
    extraLarge = RoundedCornerShape(24.dp)
)
