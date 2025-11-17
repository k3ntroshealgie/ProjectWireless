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
import com.example.campusconnect1.ui.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

enum class CurrentScreen { LOGIN, REGISTER, HOME, CREATE_POST, POST_DETAIL }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf(startScreen) }

                    // 2. Variabel untuk menyimpan ID postingan yang diklik
                    var selectedPostId by remember { mutableStateOf("") }

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
                                onFabClick = { currentScreen = CurrentScreen.CREATE_POST },
                                onLogout = { currentScreen = CurrentScreen.LOGIN },

                                // 👇 UPDATE DI SINI: Menerima klik postingan
                                onPostClick = { postId ->
                                    selectedPostId = postId // Simpan ID
                                    currentScreen = CurrentScreen.POST_DETAIL // Pindah Layar
                                }
                            )
                        }
                        CurrentScreen.CREATE_POST -> {
                            CreatePostScreen(
                                onPostSuccess = { currentScreen = CurrentScreen.HOME },
                                onBack = { currentScreen = CurrentScreen.HOME }
                            )
                        }

                        // 3. LAYAR DETAIL BARU
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