package com.example.campusconnect1.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.presentation.components.ModernPostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFabClick: () -> Unit,
    onLogout: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onUniversityChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val posts by viewModel.filteredPosts.collectAsState()
    val currentUniversity by viewModel.currentViewUniversity.collectAsState()
    val canPost by viewModel.canPost.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUserData.collectAsState()
    
    // State for features
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val currentSortType by viewModel.currentSortType.collectAsState()

    var showUniDropdown by remember { mutableStateOf(false) }

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
                    showDeletePostDialog?.let { viewModel.deletePost(it) }
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
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditPostDialog?.let { viewModel.updatePost(it, editPostText) }
                    showEditPostDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPostDialog = null }) { Text("Cancel") }
            }
        )
    }
    
    val categories = listOf("All", "Academic", "News", "Event", "Confession", "Lost & Found")

    // Notify parent about university changes
    LaunchedEffect(currentUniversity) {
        onUniversityChange(currentUniversity)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { showUniDropdown = true }) {
                        Text(
                            text = "CampusConnect+",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUniversity,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Campus",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // University Dropdown
                        DropdownMenu(
                            expanded = showUniDropdown,
                            onDismissRequest = { showUniDropdown = false }
                        ) {
                            viewModel.availableUniversities.forEach { uni ->
                                DropdownMenuItem(
                                    text = { Text(uni) },
                                    onClick = {
                                        viewModel.switchCampus(uni)
                                        showUniDropdown = false
                                    },
                                    trailingIcon = {
                                        if (uni == currentUniversity) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        val trendingTags = listOf("#BeasiswaLPDP", "#InfoMagang", "#UjianTengahSemester", "#EventCampus", "#KarirTech")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            // Trending Topics
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trendingTags) { tag ->
                    SuggestionChip(
                        onClick = { /* TODO: Filter by tag */ },
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
            
            Divider()
            
            // 1. Category Filters (LazyRow)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategoryFilterChange(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            // 2. Sort Options & Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${posts.size} posts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = { viewModel.switchSortType(SortType.POPULAR) },
                        label = { Text("Popular") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (currentSortType == SortType.POPULAR) 
                                MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ),
                        border = if (currentSortType == SortType.POPULAR) null 
                            else SuggestionChipDefaults.suggestionChipBorder(enabled = true)
                    )
                    SuggestionChip(
                        onClick = { viewModel.switchSortType(SortType.NEWEST) },
                        label = { Text("Newest") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (currentSortType == SortType.NEWEST) 
                                MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ),
                        border = if (currentSortType == SortType.NEWEST) null 
                            else SuggestionChipDefaults.suggestionChipBorder(enabled = true)
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // 3. Post List
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && posts.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (posts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No posts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Be the first to post in your community!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshPosts() }) {
                            Text("Refresh")
                        }
                    }
                } else {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    
                    // Infinite Scroll Logic
                    val reachedBottom by remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                            lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
                        }
                    }
                    
                    LaunchedEffect(reachedBottom) {
                        if (reachedBottom) {
                            viewModel.loadMorePosts()
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(posts) { post ->
                            val isBookmarked = currentUser?.savedPostIds?.contains(post.postId) == true
                            
                            Box(modifier = Modifier.clickable { onPostClick(post.postId) }) {
                                ModernPostCard(
                                    post = post,
                                    currentUserId = currentUser?.uid,
                                    onUpvoteClick = { viewModel.toggleLike(it) },
                                    onCommentClick = { onPostClick(it.postId) },
                                    isBookmarked = isBookmarked,
                                    onBookmarkClick = { viewModel.toggleBookmark(it) },
                                    onEditClick = {
                                        editPostText = it.text
                                        showEditPostDialog = it
                                    },
                                    onDeleteClick = { showDeletePostDialog = it }
                                )
                            }
                        }
                        
                        // Bottom Loading Indicator
                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
