package com.example.campusconnect1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.campusconnect1.ui.*
import com.example.campusconnect1.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

enum class CurrentScreen { LOGIN, REGISTER, HOME, CREATE_POST, POST_DETAIL, PROFILE, GROUP_LIST, GROUP_FEED }

data class BottomNavItem(val screen: CurrentScreen, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = NeoPrimary,
                    secondary = NeoSecondary,
                    background = NeoBackground,
                    surface = NeoCard
                )
            ) {
                var currentScreen by remember { mutableStateOf(startScreen) }
                var selectedPostId by remember { mutableStateOf("") }
                var selectedGroupId by remember { mutableStateOf<String?>(null) }
                var targetGroupUni by remember { mutableStateOf("") }

                val navItems = listOf(
                    BottomNavItem(CurrentScreen.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
                    BottomNavItem(CurrentScreen.GROUP_LIST, "Groups", Icons.Outlined.List, Icons.Filled.List),
                    BottomNavItem(CurrentScreen.PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
                )
                val showBottomBar = currentScreen in listOf(CurrentScreen.HOME, CurrentScreen.GROUP_LIST, CurrentScreen.PROFILE)

                Scaffold(
                    containerColor = NeoBackground,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = NeoCard, tonalElevation = 8.dp) {
                                navItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = currentScreen == item.screen,
                                        onClick = { currentScreen = item.screen },
                                        label = { Text(item.label) },
                                        icon = { Icon(if (currentScreen == item.screen) item.selectedIcon else item.icon, item.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = NeoSecondary.copy(alpha = 0.2f),
                                            selectedIconColor = NeoPrimary,
                                            selectedTextColor = NeoPrimary,
                                            unselectedIconColor = NeoTextLight,
                                            unselectedTextColor = NeoTextLight
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = NeoBackground) {
                        when (currentScreen) {
                            CurrentScreen.LOGIN -> LoginScreen(
                                onLoginSuccess = { currentScreen = CurrentScreen.HOME },
                                onNavigateToRegister = { currentScreen = CurrentScreen.REGISTER }
                            )
                            CurrentScreen.REGISTER -> RegisterScreen(
                                onRegisterSuccess = { currentScreen = CurrentScreen.HOME },
                                onNavigateToLogin = { currentScreen = CurrentScreen.LOGIN }
                            )
                            CurrentScreen.HOME -> HomeScreen(
                                onFabClick = { selectedGroupId = null; currentScreen = CurrentScreen.CREATE_POST },
                                onLogout = { currentScreen = CurrentScreen.LOGIN },
                                onPostClick = { postId -> selectedPostId = postId; currentScreen = CurrentScreen.POST_DETAIL },
                                onProfileClick = { currentScreen = CurrentScreen.PROFILE },

                                // 👇 INI YANG BENAR: Menerima update universitas dari HomeScreen
                                onUniversityChange = { uni ->
                                    targetGroupUni = uni
                                }
                            )

                            CurrentScreen.GROUP_LIST -> GroupListScreen(
                                targetUniversityId = if (targetGroupUni.isNotEmpty()) targetGroupUni else "ITB",
                                onBack = { currentScreen = CurrentScreen.HOME },
                                onGroupClick = { groupId -> selectedGroupId = groupId; currentScreen = CurrentScreen.GROUP_FEED }
                            )

                            CurrentScreen.PROFILE -> ProfileScreen(onBack = { currentScreen = CurrentScreen.HOME })

                            CurrentScreen.CREATE_POST -> CreatePostScreen(
                                groupId = selectedGroupId,
                                onPostSuccess = { if (selectedGroupId != null) currentScreen = CurrentScreen.GROUP_FEED else currentScreen = CurrentScreen.HOME },
                                onBack = { if (selectedGroupId != null) currentScreen = CurrentScreen.GROUP_FEED else currentScreen = CurrentScreen.HOME }
                            )
                            CurrentScreen.POST_DETAIL -> PostDetailScreen(
                                postId = selectedPostId,
                                onBack = { currentScreen = CurrentScreen.HOME }
                            )
                            CurrentScreen.GROUP_FEED -> {
                                if (selectedGroupId != null) {
                                    GroupFeedScreen(
                                        groupId = selectedGroupId!!,
                                        onBack = { selectedGroupId = null; currentScreen = CurrentScreen.GROUP_LIST },
                                        onCreatePostClick = { currentScreen = CurrentScreen.CREATE_POST },
                                        onPostClick = { postId -> selectedPostId = postId; currentScreen = CurrentScreen.POST_DETAIL }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}