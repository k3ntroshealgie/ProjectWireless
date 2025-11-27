package com.example.campusconnect1.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.data.model.Group
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User
import com.example.campusconnect1.presentation.group.GroupViewModel
import com.example.campusconnect1.presentation.home.HomeViewModel
import com.example.campusconnect1.presentation.components.PostCard
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class SearchType { POSTS, USERS, GROUPS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    currentUniversityId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    val posts by homeViewModel.filteredPosts.collectAsState()
    val users by homeViewModel.allUsersInUni.collectAsState()
    val groups by homeViewModel.allGroupsInUni.collectAsState()
    val searchText by homeViewModel.searchText.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Dialog States
    var showDeletePostDialog by remember { mutableStateOf<Post?>(null) }
    var showEditPostDialog by remember { mutableStateOf<Post?>(null) }
    var editPostText by remember { mutableStateOf("") }

    if (showDeletePostDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeletePostDialog = null },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeletePostDialog?.let { homeViewModel.deletePost(it) }
                    showDeletePostDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePostDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showEditPostDialog != null) {
        AlertDialog(
            onDismissRequest = { showEditPostDialog = null },
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
                    showEditPostDialog?.let { homeViewModel.updatePost(it, editPostText) }
                    showEditPostDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPostDialog = null }) { Text("Cancel") }
            }
        )
    }

    var selectedTab by remember { mutableStateOf(SearchType.POSTS) }
    val tabTitles = listOf("Posts", "Users", "Groups")

    LaunchedEffect(currentUniversityId) {
        homeViewModel.fetchAllUsersAndGroups(currentUniversityId)
    }

    // Local filtering for Users and Groups
    val filteredUsers = users.filter {
        it.fullName.contains(searchText, ignoreCase = true) ||
                it.nim.contains(searchText, ignoreCase = true) ||
                it.major.contains(searchText, ignoreCase = true)
    }

    val filteredGroups = groups.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.description.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = { homeViewModel.onSearchTextChange(it) },
                        placeholder = {
                            Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { homeViewModel.onSearchTextChange("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Companion.Transparent,
                            unfocusedContainerColor = Color.Companion.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Companion.Transparent,
                            unfocusedIndicatorColor = Color.Companion.Transparent
                        ),
                        modifier = Modifier.Companion.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.Companion.padding(padding).fillMaxSize()) {

            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.Companion.tabIndicatorOffset(selectedTab.ordinal),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab.ordinal == index,
                        onClick = { selectedTab = SearchType.entries[index] },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(top = 8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    SearchType.POSTS -> {
                        if (posts.isEmpty()) {
                            item { EmptyState("No posts found.") }
                        } else {
                            items(posts) { post ->
                                PostCard(
                                    post = post,
                                    currentUserId = currentUserId,
                                    onLikeClick = { homeViewModel.toggleLike(it) },
                                    onCommentClick = { onPostClick(it.postId) },
                                    isBookmarked = false,
                                    onBookmarkClick = { /* TODO */ },
                                    onEditClick = {
                                        editPostText = it.text
                                        showEditPostDialog = it
                                    },
                                    onDeleteClick = { showDeletePostDialog = it },
                                    modifier = Modifier.Companion.clickable { onPostClick(post.postId) }
                                )
                            }
                        }
                    }

                    SearchType.USERS -> {
                        if (filteredUsers.isEmpty()) {
                            item { EmptyState("No users found.") }
                        } else {
                            items(filteredUsers) { user ->
                                UserItem(user)
                            }
                        }
                    }

                    SearchType.GROUPS -> {
                        if (filteredGroups.isEmpty()) {
                            item { EmptyState("No groups found.") }
                        } else {
                            items(filteredGroups) { group ->
                                GroupSearchItem(group)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Companion.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserItem(user: User) {
    ListItem(
        headlineContent = {
            Text(
                user.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Companion.Bold
            )
        },
        supportingContent = {
            val subText = if (user.major.isNotEmpty()) user.major else user.nim
            Text("$subText @ ${user.universityId}")
        },
        leadingContent = {
            Box(
                modifier = Modifier.Companion
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = user.fullName.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Companion.Bold
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun GroupSearchItem(group: Group) {
    ListItem(
        headlineContent = {
            Text(
                group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Companion.Bold
            )
        },
        supportingContent = { Text("${group.memberCount} members") },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.Companion
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}