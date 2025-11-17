package com.example.campusconnect1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onFabClick: () -> Unit,
    onLogout: () -> Unit,
    // 👇 Parameter baru untuk menangani klik pada postingan (pindah ke detail)
    onPostClick: (String) -> Unit
) {
    // Mengambil state dari ViewModel
    val posts by homeViewModel.posts.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val currentUni by homeViewModel.currentViewUniversity.collectAsState()
    val canPost by homeViewModel.canPost.collectAsState()
    val currentSort by homeViewModel.currentSortType.collectAsState()

    val universities = homeViewModel.availableUniversities
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                // 1. TOP BAR UTAMA
                TopAppBar(
                    title = {
                        // Judul yang bisa diklik untuk ganti kampus (Dropdown)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { expanded = true }
                                .padding(8.dp)
                        ) {
                            Text(text = currentUni, style = MaterialTheme.typography.titleLarge)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Campus")

                            // Menu Dropdown Kampus
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                universities.forEach { uni ->
                                    DropdownMenuItem(
                                        text = { Text(uni) },
                                        onClick = {
                                            homeViewModel.switchCampus(uni)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        // Tombol Refresh Manual
                        IconButton(onClick = { homeViewModel.fetchPosts() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        // Tombol Logout
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )

                // 2. BARIS FILTER (Populer / Terbaru)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filter Popular
                    FilterChip(
                        selected = currentSort == SortType.POPULAR,
                        onClick = { homeViewModel.switchSortType(SortType.POPULAR) },
                        label = { Text("Popular 🔥") },
                        leadingIcon = if (currentSort == SortType.POPULAR) {
                            { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )

                    // Filter Newest
                    FilterChip(
                        selected = currentSort == SortType.NEWEST,
                        onClick = { homeViewModel.switchSortType(SortType.NEWEST) },
                        label = { Text("Newest 🕒") }
                    )
                }
                Divider()
            }
        },
        floatingActionButton = {
            // Tombol (+) hanya muncul jika user punya hak akses di kampus ini
            if (canPost) {
                FloatingActionButton(onClick = onFabClick) {
                    Icon(Icons.Default.Add, contentDescription = "New Post")
                }
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (posts.isEmpty()) {
                    // Tampilan jika tidak ada postingan
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No posts yet in $currentUni.")
                        if (canPost) Text("Be the first to post!")
                    }
                } else {
                    // DAFTAR POSTINGAN
                    LazyColumn {
                        items(posts) { post ->
                            // Bungkus PostCard agar bisa diklik untuk melihat detail
                            Box(modifier = Modifier.clickable {
                                onPostClick(post.postId) // Kirim ID ke MainActivity
                            }) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { selectedPost ->
                                        homeViewModel.toggleLike(selectedPost)
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