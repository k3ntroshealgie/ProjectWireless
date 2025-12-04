package com.example.campusconnect1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusconnect1.presentation.auth.LoginScreen
import com.example.campusconnect1.presentation.auth.RegisterScreen
import com.example.campusconnect1.presentation.group.GroupFeedScreen
import com.example.campusconnect1.presentation.group.GroupListScreen
import com.example.campusconnect1.presentation.home.HomeScreen
import com.example.campusconnect1.presentation.post.CreatePostScreen
import com.example.campusconnect1.presentation.post.PostDetailScreen
import com.example.campusconnect1.presentation.profile.ProfileScreen
import com.example.campusconnect1.presentation.search.SearchScreen
import com.example.campusconnect1.presentation.components.ModernBottomNavBar
import com.example.campusconnect1.ui.*
import com.example.campusconnect1.ui.theme.CampusConnect1Theme
import com.google.firebase.auth.FirebaseAuth

// Enum untuk daftar layar yang tersedia
enum class CurrentScreen { LOGIN, REGISTER, FORGOT_PASSWORD, HOME, FOR_YOU, CREATE_POST, POST_DETAIL, PROFILE, GROUP_LIST, GROUP_FEED, SEARCH, MESSAGES }

// Data class untuk item Bottom Navigation
data class BottomNavItem(
    val screen: CurrentScreen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ FIX: Enable Firestore offline persistence
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        val auth = FirebaseAuth.getInstance()
        // Cek Login: Jika user ada -> Home, jika tidak -> Login
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            // 👇 PERBAIKAN 1: Panggil Theme tanpa parameter 'darkTheme'
            CampusConnect1Theme {
                // State Navigasi Utama
                var currentScreen by remember { mutableStateOf(startScreen) }

                // State Data Sementara (untuk oper-operan data antar layar)
                var selectedPostId by remember { mutableStateOf("") }
                var selectedGroupId by remember { mutableStateOf<String?>(null) }

                // Default ke ITB, tapi nanti diupdate oleh HomeScreen
                var targetGroupUni by remember { mutableStateOf("ITB") }


                // Determine bottom nav selected index
                val bottomNavIndex = when (currentScreen) {
                    CurrentScreen.HOME -> 0
                    CurrentScreen.FOR_YOU -> 1
                    CurrentScreen.GROUP_LIST -> 2  // Groups becomes index 2 (skipping Create at index 2)
                    CurrentScreen.MESSAGES -> 3
                    CurrentScreen.PROFILE -> 4
                    else -> -1
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (bottomNavIndex >= 0) {
                            ModernBottomNavBar(
                                selectedItem = bottomNavIndex,
                                onHomeClick = { currentScreen = CurrentScreen.HOME },
                                onForYouClick = { currentScreen = CurrentScreen.FOR_YOU },
                                onCreateClick = {
                                    selectedGroupId = null
                                    currentScreen = CurrentScreen.CREATE_POST
                                },
                                onMessagesClick = { currentScreen = CurrentScreen.MESSAGES },
                                onProfileClick = { currentScreen = CurrentScreen.PROFILE }
                            )
                        }
                    }
                ) { innerPadding ->
                    // Konten Utama
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            // --- AUTHENTICATION ---
                            CurrentScreen.LOGIN -> {
                                LoginScreen(
                                    onLoginSuccess = { currentScreen = CurrentScreen.HOME },
                                    onNavigateToRegister = {
                                        currentScreen = CurrentScreen.REGISTER
                                    },
                                    onNavigateToForgotPassword = {
                                        currentScreen = CurrentScreen.FORGOT_PASSWORD
                                    }
                                )
                            }
                            CurrentScreen.REGISTER -> {
                                RegisterScreen(
                                    onRegisterSuccess = { currentScreen = CurrentScreen.HOME },
                                    onNavigateToLogin = { currentScreen = CurrentScreen.LOGIN }
                                )
                            }
                            CurrentScreen.FORGOT_PASSWORD -> {
                                com.example.campusconnect1.presentation.auth.ForgotPasswordScreen(
                                    onBack = { currentScreen = CurrentScreen.LOGIN }
                                )
                            }

                            // --- HOME ---
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

                                    // Update kampus target untuk tab Groups
                                    onUniversityChange = { uni ->
                                        targetGroupUni = uni
                                    },

                                    // Navigasi ke Layar Pencarian
                                    onSearchClick = {
                                        currentScreen = CurrentScreen.SEARCH
                                    }
                                )
                            }

                            // --- SEARCH ---
                            CurrentScreen.SEARCH -> {
                                SearchScreen(
                                    currentUniversityId = targetGroupUni,
                                    onBack = { currentScreen = CurrentScreen.HOME },
                                    onPostClick = { postId ->
                                        selectedPostId = postId
                                        currentScreen = CurrentScreen.POST_DETAIL
                                    }
                                )
                            }

                            // --- GROUPS ---
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

                            // --- FOR YOU ---
                            CurrentScreen.FOR_YOU -> {
                                // Placeholder for For You screen
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "For You Feed",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Coming soon",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // --- MESSAGES ---
                            CurrentScreen.MESSAGES -> {
                                // Placeholder for Messages screen
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Messages",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Coming soon",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // --- PROFILE ---
                            CurrentScreen.PROFILE -> {
                                // 👇 PERBAIKAN 2: Hapus parameter isDarkTheme & onThemeChange
                                ProfileScreen(
                                    onBack = { currentScreen = CurrentScreen.HOME },
                                    onPostClick = { postId ->
                                        selectedPostId = postId
                                        currentScreen = CurrentScreen.POST_DETAIL
                                    },
                                    onLogout = { currentScreen = CurrentScreen.LOGIN } // ✅ Sign out redirects to login
                                )
                            }

                            // --- CREATE & DETAIL ---
                            CurrentScreen.CREATE_POST -> {
                                CreatePostScreen(
                                    groupId = selectedGroupId,
                                    onPostSuccess = {
                                        if (selectedGroupId != null) currentScreen =
                                            CurrentScreen.GROUP_FEED
                                        else currentScreen = CurrentScreen.HOME
                                    },
                                    onBack = {
                                        if (selectedGroupId != null) currentScreen =
                                            CurrentScreen.GROUP_FEED
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