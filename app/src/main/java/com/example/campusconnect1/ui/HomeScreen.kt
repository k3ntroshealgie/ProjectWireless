// HomeScreen.kt
package com.example.campusconnect1.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp // Ikon Keluar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth // Import Auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onFabClick: () -> Unit,
    onLogout: () -> Unit // 👇 Tambah parameter ini untuk aksi logout
) {
    val posts by homeViewModel.posts.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusConnect+") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Tombol Refresh
                    IconButton(onClick = { homeViewModel.fetchPosts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    // 👇 TOMBOL LOGOUT BARU
                    IconButton(onClick = {
                        // 1. Hapus sesi Firebase
                        FirebaseAuth.getInstance().signOut()
                        // 2. Pindah layar
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(Icons.Default.Add, contentDescription = "New Post")
            }
        }
    ) { paddingValues ->
        // ... (Bagian Box ke bawah SAMA PERSIS, tidak perlu diubah) ...
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (posts.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No posts yet.")
                        Text("Be the first to write something!")
                    }
                } else {
                    LazyColumn {
                        items(posts) { post ->
                            PostCard(post = post)
                        }
                    }
                }
            }
        }
    }
}