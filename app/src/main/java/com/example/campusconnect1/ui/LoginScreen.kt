package com.example.campusconnect1.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.auth.AuthResult
import com.example.campusconnect1.auth.AuthState
import com.example.campusconnect1.auth.AuthViewModel
import com.example.campusconnect1.ui.theme.NeoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by authViewModel.authResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        when (authState.state) {
            AuthState.SUCCESS -> {
                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
                authViewModel.resetState()
            }
            AuthState.ERROR -> {
                Toast.makeText(context, "Login Failed: ${authState.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Judul Aplikasi
        Text(
            text = "CampusConnect+",
            style = MaterialTheme.typography.headlineLarge,
            color = NeoPrimary
        )
        Text(
            text = "Login to your community",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Input Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Login
        Button(
            onClick = { authViewModel.loginUser(email, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = authState.state != AuthState.LOADING,
            colors = ButtonDefaults.buttonColors(containerColor = NeoPrimary)
        ) {
            if (authState.state == AuthState.LOADING) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Logging in...")
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link ke Register
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register here")
        }

    }
}