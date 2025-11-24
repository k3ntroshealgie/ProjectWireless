package com.example.campusconnect1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning // Import icon warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter // Import painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest // Import ImageRequest
import com.example.campusconnect1.Comment
import com.example.campusconnect1.ui.theme.* // Import tema warna Neo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    viewModel: PostDetailViewModel = viewModel()
) {
    // Load data saat layar dibuka
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    val post by viewModel.selectedPost.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    val context = LocalContext.current // Context untuk Coil Image Loader

    Scaffold(
        containerColor = NeoBackground, // Pakai warna background tema
        topBar = {
            TopAppBar(
                title = { Text("Post Details", color = NeoText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeoText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeoCard)
            )
        },
        bottomBar = {
            // --- AREA INPUT KOMENTAR ---
            Surface(
                color = NeoCard,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a comment...", color = NeoTextLight) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeoPrimary,
                            unfocusedBorderColor = NeoTextLight.copy(alpha = 0.5f),
                            focusedTextColor = NeoText
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.sendComment(postId, commentText)
                            commentText = "" // Kosongkan input
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) NeoPrimary else NeoTextLight
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. TAMPILKAN POSTINGAN UTAMA
            item {
                post?.let { postData ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NeoCard),
                        shape = RoundedCornerShape(0.dp), // Flat style agar menyatu
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header (Avatar + Nama)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (postData.authorAvatarUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(postData.authorAvatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(NeoSecondary.copy(alpha=0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(postData.authorName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = NeoPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = postData.authorName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoText
                                    )
                                    Text(
                                        text = postData.universityId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeoTextLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Isi Teks
                            Text(
                                text = postData.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = NeoText
                            )

                            // 👇👇 PERBAIKAN GAMBAR DISINI 👇👇
                            if (postData.imageUrl != null && postData.imageUrl.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(postData.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Post Image Detail",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight() // Tinggi menyesuaikan
                                        .heightIn(min = 200.dp, max = 500.dp) // Batas tinggi
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray), // Placeholder warna
                                    contentScale = ContentScale.FillWidth, // Isi lebar penuh

                                    // Tampilkan ikon warning jika error
                                    error = rememberVectorPainter(Icons.Default.Warning)
                                )
                            }
                            // 👆👆 SELESAI PERBAIKAN 👆👆

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = NeoTextLight.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${comments.size} Comments",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeoTextLight
                            )
                        }
                    }
                }
            }

            // 2. DAFTAR KOMENTAR
            items(comments) { comment ->
                CommentItem(comment)
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 1.dp), // Flat list style
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = NeoCard)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NeoText)
                Spacer(modifier = Modifier.weight(1f))
                // Date placeholder if needed
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, style = MaterialTheme.typography.bodyMedium, color = NeoText)
            Divider(modifier = Modifier.padding(top = 12.dp), color = NeoTextLight.copy(alpha = 0.1f))
        }
    }
}