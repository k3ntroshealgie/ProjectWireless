package com.example.campusconnect1.ui

import androidx.compose.foundation.BorderStroke // 👈 Fix error BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape // 👈 Fix error RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.campusconnect1.Post
import com.example.campusconnect1.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PostCard(
    post: Post,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onEditClick: (Post) -> Unit = {},
    onDeleteClick: (Post) -> Unit = {}
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isLiked = post.likedBy.contains(currentUserId)
    val isOwner = currentUserId == post.authorId
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp), // Separator tipis ala Reddit
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = NeoCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- HEADER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.authorAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(post.authorAvatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(NeoSecondary.copy(alpha=0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.authorName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = NeoPrimary)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "c/${post.universityId}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = NeoText
                    )
                    Text(
                        text = "u/${post.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeoTextLight
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = NeoTextLight)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            // 👇 PERBAIKAN: Hapus containerColor, pakai Modifier.background
                            modifier = Modifier.background(NeoCard)
                        ) {
                            DropdownMenuItem(text = { Text("Edit", color = NeoText) }, onClick = { onEditClick(post); showMenu = false })
                            DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { onDeleteClick(post); showMenu = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- CONTENT TEXT ---
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyMedium,
                color = NeoText,
                lineHeight = 22.sp
            )

            // --- CONTENT IMAGE ---
            if (post.imageUrl != null && post.imageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = ImageRequest.Builder(context).data(post.imageUrl).crossfade(true).build(),
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NeoTextLight.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- ACTION BAR (Buttons) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                // 1. VOTE PILL
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeoBackground,
                    // 👇 MENGGUNAKAN BORDER STROKE YANG SUDAH DIIMPORT
                    border = if (isLiked) BorderStroke(1.dp, NeoPrimary.copy(alpha=0.5f)) else null,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        IconButton(onClick = { onLikeClick(post) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Upvote",
                                tint = if (isLiked) Color.Red else NeoTextLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "${post.voteCount}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiked) Color.Red else NeoTextLight,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. COMMENT BUTTON
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeoBackground,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { onCommentClick(post) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "💬 ${post.commentCount} Comments",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeoTextLight
                        )
                    }
                }
            }
        }
    }
}