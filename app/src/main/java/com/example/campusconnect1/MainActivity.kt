package com.example.campusconnect1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.campusconnect1.ui.CreatePostScreen
import com.example.campusconnect1.ui.GroupFeedScreen
import com.example.campusconnect1.ui.GroupListScreen
import com.example.campusconnect1.ui.HomeScreen
import com.example.campusconnect1.ui.LoginScreen
import com.example.campusconnect1.ui.PostDetailScreen
import com.example.campusconnect1.ui.ProfileScreen
import com.example.campusconnect1.ui.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

enum class CurrentScreen { LOGIN, REGISTER, HOME, CREATE_POST, POST_DETAIL, PROFILE, GROUP_LIST, GROUP_FEED }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(startScreen) }

                    var selectedPostId by remember { mutableStateOf("") }
                    var selectedGroupId by remember { mutableStateOf<String?>(null) }
                    // 👇 Variable Baru: Menyimpan ID Kampus target untuk Grup
                    var targetGroupUni by remember { mutableStateOf("") }

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

                                // 👇 UPDATE: Tangkap uniId, simpan, lalu pindah
                                onGroupClick = { uniId ->
                                    targetGroupUni = uniId
                                    currentScreen = CurrentScreen.GROUP_LIST
                                }
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
                        CurrentScreen.PROFILE -> {
                            ProfileScreen(
                                onBack = { currentScreen = CurrentScreen.HOME }
                            )
                        }

                        // 👇 LAYAR GROUP LIST
                        CurrentScreen.GROUP_LIST -> {
                            GroupListScreen(
                                // 👇 Kirim kampus target ke sini
                                targetUniversityId = targetGroupUni,
                                onBack = { currentScreen = CurrentScreen.HOME },
                                onGroupClick = { groupId ->
                                    selectedGroupId = groupId
                                    currentScreen = CurrentScreen.GROUP_FEED
                                }
                            )
                        }

                        // 👇 LAYAR GROUP FEED
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
                    }
                }
            }
        }
    }
}