package com.example.campusconnect1.presentation.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.data.model.Comment
import com.example.campusconnect1.presentation.components.PostCard
import com.example.campusconnect1.presentation.post.PostDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onPostUpdated: () -> Unit = {},  // ← ADD THIS LINE
    viewModel: PostDetailViewModel = viewModel()
) {
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    val post by viewModel.selectedPost.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // State for Dialogs
    var showDeletePostDialog by remember { mutableStateOf(false) }
    var showEditPostDialog by remember { mutableStateOf(false) }
    var editPostText by remember { mutableStateOf("") }

    var showDeleteCommentDialog by remember { mutableStateOf<Comment?>(null) }
    var showEditCommentDialog by remember { mutableStateOf<Comment?>(null) }
    var editCommentText by remember { mutableStateOf("") }

    // --- DIALOGS ---

    if (showDeletePostDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePostDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost(postId) {
                        showDeletePostDialog = false
                        onBack()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePostDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditPostDialog) {
        AlertDialog(
            onDismissRequest = { showEditPostDialog = false },
            title = { Text("Edit Post") },
            text = {
                OutlinedTextField(
                    value = editPostText,
                    onValueChange = { editPostText = it },
                    label = { Text("Post Content") },
                    modifier = Modifier.Companion.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editPost(postId, editPostText)
                    showEditPostDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPostDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteCommentDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = null },
            title = { Text("Delete Comment") },
            text = { Text("Are you sure you want to delete this comment?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCommentDialog?.let { viewModel.deleteComment(postId, it.id) }
                    showDeleteCommentDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCommentDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showEditCommentDialog != null) {
        AlertDialog(
            onDismissRequest = { showEditCommentDialog = null },
            title = { Text("Edit Comment") },
            text = {
                OutlinedTextField(
                    value = editCommentText,
                    onValueChange = { editCommentText = it },
                    label = { Text("Comment") },
                    modifier = Modifier.Companion.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditCommentDialog?.let {
                        viewModel.editComment(
                            postId,
                            it.id,
                            editCommentText
                        )
                    }
                    showEditCommentDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditCommentDialog = null }) { Text("Cancel") }
            }
        )
    }

    // --- UI CONTENT ---

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Post Details", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a comment...") },
                        modifier = Modifier.Companion.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            ),
                            focusedBorderColor = Color.Companion.Transparent,
                            unfocusedBorderColor = Color.Companion.Transparent
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.Companion.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.sendComment(postId, commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.38f
                            )
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.Companion
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                post?.let { postData ->
                    PostCard(
                        post = postData,
                        currentUserId = currentUserId,
                        onLikeClick = { 
                            viewModel.toggleLike(it)
                            onPostUpdated() // ✅ Notify changes
                        },
                        onCommentClick = { }, // Already on detail screen
                        onEditClick = {
                            editPostText = it.text
                            showEditPostDialog = true
                        },
                        onDeleteClick = { showDeletePostDialog = true }
                    )

                    Text(
                        "Comments (${comments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.Companion.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    currentUserId = currentUserId,
                    onLikeClick = { viewModel.toggleLikeComment(postId, it) },
                    onEditClick = {
                        editCommentText = it.text
                        showEditCommentDialog = it
                    },
                    onDeleteClick = { showDeleteCommentDialog = it }
                )
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onLikeClick: (Comment) -> Unit,
    onEditClick: (Comment) -> Unit,
    onDeleteClick: (Comment) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isLiked = comment.likedBy.contains(currentUserId)

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                Text(
                    comment.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Companion.Bold
                )
                Spacer(modifier = Modifier.Companion.width(8.dp))
                // TODO: Add timestamp
            }
        },
        supportingContent = {
            Column {
                Text(
                    comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Comment Actions (Like)
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    modifier = Modifier.Companion.padding(top = 4.dp)
                ) {
                    Text(
                        "Like",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isLiked) FontWeight.Companion.Bold else FontWeight.Companion.Normal,
                        modifier = Modifier.Companion.clickable { onLikeClick(comment) }
                    )
                    if (comment.voteCount > 0) {
                        Spacer(modifier = Modifier.Companion.width(4.dp))
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.Companion.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.Companion.width(2.dp))
                        Text(
                            "${comment.voteCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier.Companion
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = comment.authorName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        trailingContent = {
            if (currentUserId == comment.authorId) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.Companion.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.Companion.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEditClick(comment)
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDeleteClick(comment)
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}