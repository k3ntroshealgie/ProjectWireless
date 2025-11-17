package com.example.campusconnect1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,            // ID passed from Home
    onBack: () -> Unit,        // Callback to go back
    viewModel: PostDetailViewModel = viewModel()
) {
    // Load data when screen launches
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    val post by viewModel.selectedPost.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // --- COMMENT INPUT AREA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        viewModel.sendComment(postId, commentText)
                        commentText = "" // Clear input
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. SHOW THE POST ITSELF (Reusing PostCard logic manually or simplified)
            item {
                post?.let {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(it.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(it.text, style = MaterialTheme.typography.bodyLarge)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("${comments.size} Comments", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // 2. LIST OF COMMENTS
            items(comments) { comment ->
                CommentItem(comment)
            }
        }
    }
}

@Composable
fun CommentItem(comment: com.example.campusconnect1.Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                // Format Date (Optional)
                val dateStr = comment.timestamp?.toString() ?: ""
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}