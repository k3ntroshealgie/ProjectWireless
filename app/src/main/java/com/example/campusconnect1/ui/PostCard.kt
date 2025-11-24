package com.example.campusconnect1.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark // Icon Penuh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder // Icon Garis
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
    onDeleteClick: (Post) -> Unit = {},
    onBookmarkClick: (Post) -> Unit = {},
    isBookmarked: Boolean = false // Status apakah sudah disimpan
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isLiked = post.likedBy.contains(currentUserId)
    val isOwner = currentUserId == post.authorId
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // 👇 FIX
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // HEADER
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
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), // 👇 FIX
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.authorName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "c/${post.universityId}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface // 👇 FIX
                    )
                    Text(
                        text = "u/${post.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 👇 FIX
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { onBookmarkClick(post) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) NeoPrimary else NeoTextLight // Biru jika disimpan
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface) // 👇 FIX
                        ) {
                            DropdownMenuItem(text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) }, onClick = { onEditClick(post); showMenu = false })
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteClick(post); showMenu = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LABEL KATEGORI
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer, // 👇 FIX
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = post.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer // 👇 FIX
                )
            }

            // CONTENT TEXT
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, // 👇 FIX
                lineHeight = 22.sp
            )

            // CONTENT IMAGE
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
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ACTION BAR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                // VOTE PILL
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant, // 👇 FIX
                    border = if (isLiked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f)) else null,
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
                                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "${post.voteCount}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // COMMENT BUTTON
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant, // 👇 FIX
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}