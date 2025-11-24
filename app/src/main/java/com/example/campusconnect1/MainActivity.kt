package com.example.campusconnect1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme // 👈 Import ini penting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.campusconnect1.ui.*
import com.example.campusconnect1.ui.theme.CampusConnect1Theme
import com.google.firebase.auth.FirebaseAuth

enum class CurrentScreen { LOGIN, REGISTER, HOME, CREATE_POST, POST_DETAIL, PROFILE, GROUP_LIST, GROUP_FEED }

data class BottomNavItem(
    val screen: CurrentScreen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            // 👇 1. STATE TEMA (Agar switch di profil berfungsi)
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            // 👇 2. Masukkan state ke Theme
            CampusConnect1Theme(darkTheme = isDarkTheme) {

                var currentScreen by remember { mutableStateOf(startScreen) }
                var selectedPostId by remember { mutableStateOf("") }
                var selectedGroupId by remember { mutableStateOf<String?>(null) }
                var targetGroupUni by remember { mutableStateOf("ITB") }

                val navItems = listOf(
                    BottomNavItem(CurrentScreen.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
                    BottomNavItem(CurrentScreen.GROUP_LIST, "Groups", Icons.Outlined.List, Icons.Filled.List),
                    BottomNavItem(CurrentScreen.PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
                )

                val showBottomBar = currentScreen in listOf(CurrentScreen.HOME, CurrentScreen.GROUP_LIST, CurrentScreen.PROFILE)

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = currentScreen == item.screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = item.screen },
                                        label = { Text(item.label) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                                contentDescription = item.label
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            CurrentScreen.LOGIN -> {
                                LoginScreen(
                                    onLoginSuccess = { currentScreen = CurrentScreen.HOME },
                                    onNavigateToRegister = { currentScreen = CurrentScreen.REGISTER }
                                )
                            }
                            CurrentScreen.REGISTER -> {
                                RegisterScreen(
                                    onRegisterSuccess = { currentScreen = CurrentScreen.HOME },
                                    onNavigateToLogin = { currentScreen = CurrentScreen.LOGIN }
                                )
                            }
                            CurrentScreen.HOME -> {
                                HomeScreen(
                                    onFabClick = {
                                        selectedGroupId = null
                                        currentScreen = CurrentScreen.CREATE_POST
                                    },
                                    onLogout = { currentScreen = CurrentScreen.LOGIN },
                                    onPostClick = { postId ->
                                        selectedPostId = postId
                                        currentScreen = CurrentScreen.POST_DETAIL
                                    },
                                    onProfileClick = { currentScreen = CurrentScreen.PROFILE },

                                    // ✅ SUDAH BENAR: onGroupClick dihapus, diganti onUniversityChange
                                    onUniversityChange = { uni ->
                                        targetGroupUni = uni
                                    }
                                )
                            }

                            CurrentScreen.GROUP_LIST -> {
                                GroupListScreen(
                                    targetUniversityId = targetGroupUni,
                                    onBack = { currentScreen = CurrentScreen.HOME },
                                    onGroupClick = { groupId ->
                                        selectedGroupId = groupId
                                        currentScreen = CurrentScreen.GROUP_FEED
                                    }
                                )
                            }

                            CurrentScreen.GROUP_FEED -> {
                                if (selectedGroupId != null) {
                                    GroupFeedScreen(
                                        groupId = selectedGroupId!!,
                                        onBack = {
                                            selectedGroupId = null
                                            currentScreen = CurrentScreen.GROUP_LIST
                                        },
                                        onCreatePostClick = { groupId ->
                                            currentScreen = CurrentScreen.CREATE_POST
                                        },
                                        onPostClick = { postId ->
                                            selectedPostId = postId
                                            currentScreen = CurrentScreen.POST_DETAIL
                                        }
                                    )
                                }
                            }

                            // 👇 3. UPDATE PROFILE: Sambungkan logic Dark Mode
                            CurrentScreen.PROFILE -> {
                                ProfileScreen(
                                    onBack = { currentScreen = CurrentScreen.HOME },
                                    isDarkTheme = isDarkTheme,
                                    onThemeChange = { isDarkTheme = it }
                                )
                            }

                            CurrentScreen.CREATE_POST -> {
                                CreatePostScreen(
                                    groupId = selectedGroupId,
                                    onPostSuccess = {
                                        if (selectedGroupId != null) currentScreen = CurrentScreen.GROUP_FEED
                                        else currentScreen = CurrentScreen.HOME
                                    },
                                    onBack = {
                                        if (selectedGroupId != null) currentScreen = CurrentScreen.GROUP_FEED
                                        else currentScreen = CurrentScreen.HOME
                                    }
                                )
                            }

                            CurrentScreen.POST_DETAIL -> {
                                PostDetailScreen(
                                    postId = selectedPostId,
                                    onBack = { currentScreen = CurrentScreen.HOME }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}