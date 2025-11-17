// MainActivity.kt
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
import com.example.campusconnect1.ui.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

enum class CurrentScreen { LOGIN, REGISTER, HOME, CREATE_POST }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val startScreen = if (auth.currentUser != null) CurrentScreen.HOME else CurrentScreen.LOGIN

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf(startScreen) }

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
                                // 👇 TANGKAP SINYAL LOGOUT DI SINI
                                onLogout = {
                                    currentScreen = CurrentScreen.LOGIN
                                }
                            )
                        }
                        CurrentScreen.CREATE_POST -> {
                            CreatePostScreen(
                                onPostSuccess = { currentScreen = CurrentScreen.HOME },
                                onBack = { currentScreen = CurrentScreen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}