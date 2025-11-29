package com.example.campusconnect1.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModernBottomNavBar(
    selectedItem: Int = 0,
    onHomeClick: () -> Unit,
    onForYouClick: () -> Unit,
    onCreateClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Bottom Navigation Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = selectedItem == 0,
                    onClick = onHomeClick
                )

                // For You
                BottomNavItem(
                    icon = Icons.Default.AutoAwesome,
                    label = "For You",
                    isSelected = selectedItem == 1,
                    onClick = onForYouClick
                )

                // Spacer for FAB
                Spacer(Modifier.width(72.dp))

                // Messages
                BottomNavItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "Messages",
                    isSelected = selectedItem == 3,
                    onClick = onMessagesClick
                )

                // Profile
                BottomNavItem(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    isSelected = selectedItem == 4,
                    onClick = onProfileClick
                )
            }
        }

        // Purple FAB in center (positioned above bottom nav)
        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 16.dp)
                .size(56.dp),
            containerColor = Color(0xFF9333EA),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF6B7280),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF6B7280),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
