package com.example.campusconnect1.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.ui.theme.*

/**
 * Modern Post Card - Reddit/LinkedIn Inspired
 * 
 * Features:
 * - Emoji avatar (40.dp circular)
 * - Verified badge (blue circle with ✓)
 * - Category pill (color-coded)
 * - Content preview (5 lines max)
 * - Image support (rounded corners)
 * - Action bar: Like | Comment | Share | Bookmark
 * - Edit/Delete menu (author only)
 * - Smooth animations
 */
@Composable
fun ModernPostCard(
    post: Post,
    currentUserId: String?,
    onUpvoteClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: ((Post) -> Unit)? = null,
    isBookmarked: Boolean = false,
    onBookmarkClick: ((Post) -> Unit)? = null,
    onEditClick: ((Post) -> Unit)? = null,
    onDeleteClick: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isLiked = post.likedBy.contains(currentUserId)
    
    // Animated colors
    val likeColor by animateColorAsState(
        targetValue = if (isLiked) Color.Red else TextSecondary,
        label = "like_color"
    )
    
    val bookmarkColor by animateColorAsState(
        targetValue = if (isBookmarked) PrimaryBlue else TextSecondary,
        label = "bookmark_color"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ===== AUTHOR INFO ROW =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Emoji Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorAvatar,
                            fontSize = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                            // Verified Badge
                            if (post.isAuthorVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(PrimaryBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.universityId,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = " • ${formatTimeAgo(post.timestamp?.time ?: 0L)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                // Edit/Delete Menu (Only for author)
                if (currentUserId == post.authorId && onEditClick != null && onDeleteClick != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = TextSecondary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEditClick(post)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    onDeleteClick(post)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ===== CATEGORY PILL =====
            Surface(
                color = getCategoryColor(post.category).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = post.category,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = getCategoryColor(post.category),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            
            // ===== TITLE (if exists) =====
            if (post.title.isNotEmpty()) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // ===== CONTENT PREVIEW =====
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            
            // ===== IMAGE (if exists) =====
            if (!post.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // ===== GROUP BADGE (if in group) =====
            if (!post.groupId.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Posted in ${post.groupId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = BorderLight)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ===== ACTION BAR =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like/Upvote
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUpvoteClick(post) }
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(20.dp),
                        tint = likeColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likedBy.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = likeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Comment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCommentClick(post) }
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment",
                        modifier = Modifier.size(20.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.commentCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Share
                if (onShareClick != null) {
                    IconButton(
                        onClick = { onShareClick(post) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(20.dp),
                            tint = TextSecondary
                        )
                    }
                }
                
                // Bookmark
                if (onBookmarkClick != null) {
                    IconButton(
                        onClick = { onBookmarkClick(post) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            modifier = Modifier.size(20.dp),
                            tint = bookmarkColor
                        )
                    }
                }
            }
        }
    }
}

// Helper function to get category color
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "scholarship", "beasiswa" -> CategoryScholarship
        "career", "karir" -> CategoryCareer
        "event", "acara" -> CategoryEvent
        "freelance" -> CategoryFreelance
        "tech", "teknologi" -> CategoryTech
        "sports", "olahraga" -> CategorySports
        "music", "musik" -> CategoryMusic
        else -> PrimaryBlue
    }
}

// Helper function for relative time
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        else -> "${diff / 604800_000}w"
    }
}
