package com.example.campusconnect1.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFeedScreen(
    groupId: String,
    onBack: () -> Unit,
    onCreatePostClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: GroupFeedViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel() // Kita pakai ini untuk cek akses (canPost)
) {
    LaunchedEffect(groupId) {
        viewModel.loadGroupData(groupId)
    }

    val groupInfo by viewModel.groupInfo.collectAsState()
    val posts by viewModel.groupPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 👇 Ambil status 'canPost' dari HomeViewModel
    // Jika di Home kita sedang mode Tamu (canPost=false), maka di Grup juga Tamu
    val canPost by homeViewModel.canPost.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(groupInfo?.name ?: "Loading...", style = MaterialTheme.typography.titleMedium)
                        Text("${groupInfo?.memberCount ?: 0} members", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            // 👇 HANYA MUNCUL JIKA BUKAN TAMU
            if (canPost) {
                FloatingActionButton(onClick = { onCreatePostClick(groupId) }) {
                    Icon(Icons.Default.Add, "Post to Group")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && groupInfo == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    // Header Info Grup
                    item {
                        if (!groupInfo?.description.isNullOrEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Text(
                                    text = groupInfo!!.description,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Postingan
                    if (posts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No posts yet in this group.")
                            }
                        }
                    } else {
                        items(posts) { post ->
                            Box(modifier = Modifier.clickable { onPostClick(post.postId) }) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { homeViewModel.toggleLike(it) },
                                    onCommentClick = { onPostClick(it.postId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}