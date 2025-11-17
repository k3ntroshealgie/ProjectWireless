package com.example.campusconnect1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Import semua icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.Post
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onFabClick: () -> Unit,
    onLogout: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    // 👇 UPDATE: Callback baru untuk membuka daftar grup
    // Mengirim String (ID Kampus yang sedang dilihat)
    onGroupClick: (String) -> Unit
) {
    // State dari ViewModel
    val posts by homeViewModel.filteredPosts.collectAsState()
    val searchText by homeViewModel.searchText.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val currentUni by homeViewModel.currentViewUniversity.collectAsState() // Kampus yang sedang dilihat
    val canPost by homeViewModel.canPost.collectAsState()
    val currentSort by homeViewModel.currentSortType.collectAsState()
    val universities = homeViewModel.availableUniversities

    var expanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    // State Dialog Edit Post
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var editTextField by remember { mutableStateOf("") }

    // Dialog Edit UI
    if (postToEdit != null) {
        AlertDialog(
            onDismissRequest = { postToEdit = null },
            title = { Text("Edit Post") },
            text = {
                OutlinedTextField(
                    value = editTextField,
                    onValueChange = { editTextField = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.updatePost(postToEdit!!, editTextField)
                    postToEdit = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { postToEdit = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            Column {
                if (isSearching) {
                    // --- SEARCH BAR MODE ---
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchText,
                                onValueChange = { homeViewModel.onSearchTextChange(it) },
                                placeholder = { Text("Search posts...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isSearching = false; homeViewModel.onSearchTextChange("") }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            if (searchText.isNotEmpty()) IconButton(onClick = { homeViewModel.onSearchTextChange("") }) { Icon(Icons.Default.Close, "Clear") }
                        }
                    )
                } else {
                    // --- NORMAL MODE ---
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { expanded = true }.padding(8.dp)
                            ) {
                                Text(text = currentUni, style = MaterialTheme.typography.titleLarge)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch")
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    universities.forEach { uni ->
                                        DropdownMenuItem(text = { Text(uni) }, onClick = { homeViewModel.switchCampus(uni); expanded = false })
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        actions = {
                            // 👇 TOMBOL GRUP (Panggil callback dengan currentUni)
                            IconButton(onClick = { onGroupClick(currentUni) }) {
                                Icon(Icons.Default.List, contentDescription = "Groups")
                            }

                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onProfileClick) {
                                Icon(Icons.Default.Person, contentDescription = "Profile")
                            }
                            IconButton(onClick = {
                                FirebaseAuth.getInstance().signOut()
                                onLogout()
                            }) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                            }
                        }
                    )
                }

                // --- FILTER CHIPS ---
                if (!isSearching) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentSort == SortType.POPULAR,
                            onClick = { homeViewModel.switchSortType(SortType.POPULAR) },
                            label = { Text("Popular 🔥") },
                            leadingIcon = if (currentSort == SortType.POPULAR) { { Icon(Icons.Default.Favorite, null, Modifier.size(18.dp)) } } else null
                        )
                        FilterChip(
                            selected = currentSort == SortType.NEWEST,
                            onClick = { homeViewModel.switchSortType(SortType.NEWEST) },
                            label = { Text("Newest 🕒") }
                        )
                    }
                    Divider()
                }
            }
        },
        floatingActionButton = {
            // FAB hanya muncul jika user punya izin posting di kampus ini (Bukan Tamu)
            if (canPost && !isSearching) {
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
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (searchText.isNotEmpty()) "No results for '$searchText'" else "No posts yet in $currentUni.")
                        if (canPost) Text("Be the first to post!")
                    }
                } else {
                    LazyColumn {
                        items(posts) { post ->
                            Box(modifier = Modifier.clickable { onPostClick(post.postId) }) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { homeViewModel.toggleLike(it) },
                                    onCommentClick = { onPostClick(it.postId) },
                                    onDeleteClick = { homeViewModel.deletePost(it) },
                                    onEditClick = { editTextField = it.text; postToEdit = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}