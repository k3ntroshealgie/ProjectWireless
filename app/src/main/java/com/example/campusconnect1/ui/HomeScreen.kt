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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
    onProfileClick: () -> Unit
) {
    val posts by homeViewModel.posts.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val currentUni by homeViewModel.currentViewUniversity.collectAsState()
    val canPost by homeViewModel.canPost.collectAsState()
    val currentSort by homeViewModel.currentSortType.collectAsState()

    val universities = homeViewModel.availableUniversities
    var expanded by remember { mutableStateOf(false) }

    // 👇 STATE UNTUK DIALOG EDIT
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var editTextField by remember { mutableStateOf("") }

    // 👇 POPUP DIALOG EDIT
    if (postToEdit != null) {
        AlertDialog(
            onDismissRequest = { postToEdit = null },
            title = { Text("Edit Post") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTextField,
                        onValueChange = { editTextField = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Panggil fungsi update di ViewModel
                    homeViewModel.updatePost(postToEdit!!, editTextField)
                    postToEdit = null // Tutup dialog
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { postToEdit = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { expanded = true }.padding(8.dp)
                        ) {
                            Text(text = currentUni, style = MaterialTheme.typography.titleLarge)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Campus")

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
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                        IconButton(onClick = { homeViewModel.fetchPosts() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentSort == SortType.POPULAR,
                        onClick = { homeViewModel.switchSortType(SortType.POPULAR) },
                        label = { Text("Popular 🔥") },
                        leadingIcon = if (currentSort == SortType.POPULAR) {
                            { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )

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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No posts yet in $currentUni.")
                        if (canPost) Text("Be the first to post!")
                    }
                } else {
                    LazyColumn {
                        items(posts) { post ->
                            Box(modifier = Modifier.clickable {
                                onPostClick(post.postId)
                            }) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { selectedPost ->
                                        homeViewModel.toggleLike(selectedPost)
                                    },
                                    onCommentClick = { selectedPost ->
                                        onPostClick(selectedPost.postId)
                                    },

                                    // 👇 SAMBUNGKAN FITUR EDIT & DELETE DI SINI
                                    onDeleteClick = { selectedPost ->
                                        homeViewModel.deletePost(selectedPost)
                                    },
                                    onEditClick = { selectedPost ->
                                        // Siapkan data untuk diedit
                                        editTextField = selectedPost.text
                                        postToEdit = selectedPost // Munculkan Dialog
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