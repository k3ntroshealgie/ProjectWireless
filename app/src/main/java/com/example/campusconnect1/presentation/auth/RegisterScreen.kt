package com.example.campusconnect1.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var universityId by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var showUniversityDropdown by remember { mutableStateOf(false) }

    val authState by authViewModel.authResult.collectAsState()
    val context = LocalContext.current

    val universities = listOf(
        "UI" to "🎓 Universitas Indonesia",
        "ITB" to "🏗️ Institut Teknologi Bandung",
        "UGM" to "⚡ Universitas Gadjah Mada",
        "IPB" to "🌾 IPB University",
        "UNAIR" to "🏥 Universitas Airlangga",
        "ITS" to "🚀 Institut Teknologi Sepuluh Nopember",
        "UNDIP" to "📚 Universitas Diponegoro",
        "UNPAD" to "🌟 Universitas Padjadjaran"
    )

    LaunchedEffect(authState) {
        when (authState.state) {
            AuthState.SUCCESS -> {
                Toast.makeText(context, authState.message, Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
                authViewModel.resetState()
            }
            AuthState.ERROR -> {
                Toast.makeText(context, "Error: ${authState.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    // Same pattern as LoginScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2563EB), // Blue-600
                        Color(0xFF1E40AF)  // Blue-700
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Logo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Card
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎓", fontSize = 36.sp)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    "Join CampusConnect+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Create your account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Form Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Fill in your details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("University Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // University Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showUniversityDropdown,
                        onExpandedChange = { showUniversityDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = universities.find { it.first == universityId }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("University") },
                            leadingIcon = { Icon(Icons.Default.School, null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUniversityDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = showUniversityDropdown,
                            onDismissRequest = { showUniversityDropdown = false }
                        ) {
                            universities.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        universityId = id
                                        showUniversityDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // NIM
                    OutlinedTextField(
                        value = nim,
                        onValueChange = { nim = it },
                        label = { Text("Student ID (NIM)") },
                        leadingIcon = { Icon(Icons.Default.Badge, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Verification Notice (RIGHT AFTER NIM)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFDEEBFF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Your NIM will be verified by admin",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    // Register Button
                    Button(
                        onClick = {
                            authViewModel.registerUser(email, password, fullName, universityId, nim)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = authState.state != AuthState.LOADING &&
                                email.isNotBlank() && password.isNotBlank() &&
                                fullName.isNotBlank() && universityId.isNotBlank() && nim.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        )
                    ) {
                        if (authState.state == AuthState.LOADING) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Creating account...", fontSize = 16.sp)
                        } else {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Login Link
                    Box(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Already have an account? ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Login",
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable { onNavigateToLogin() }
                            )
                        }
                    }
                }
            }
        }
    }
}