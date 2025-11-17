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
import com.example.campusconnect1.ui.HomeScreen
import com.example.campusconnect1.ui.LoginScreen
import com.example.campusconnect1.ui.PostDetailScreen
import com.example.campusconnect1.ui.ProfileScreen
import com.example.campusconnect1.ui.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

// Daftar semua layar yang ada di aplikasi
enum class CurrentScreen { LOGIN, REGISTER, HOME, CREATE_POST, POST_DETAIL, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        // Cek status login: Langsung ke Home jika sudah login
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(startScreen) }

                    // Variabel untuk menyimpan ID postingan yang sedang diklik
                    var selectedPostId by remember { mutableStateOf("") }

                    when (currentScreen) {
                        // --- AUTH ---
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

                        // --- UTAMA ---
                        CurrentScreen.HOME -> {
                            HomeScreen(
                                onFabClick = { currentScreen = CurrentScreen.CREATE_POST },
                                onLogout = { currentScreen = CurrentScreen.LOGIN },
                                onPostClick = { postId ->
                                    selectedPostId = postId
                                    currentScreen = CurrentScreen.POST_DETAIL
                                },
                                onProfileClick = {
                                    currentScreen = CurrentScreen.PROFILE
                                }
                            )
                        }

                        // --- FITUR ---
                        CurrentScreen.CREATE_POST -> {
                            CreatePostScreen(
                                onPostSuccess = { currentScreen = CurrentScreen.HOME },
                                onBack = { currentScreen = CurrentScreen.HOME }
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
                    }
                }
            }
        }
    }
}