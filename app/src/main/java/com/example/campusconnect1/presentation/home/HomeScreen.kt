package com.example.campusconnect1.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.presentation.components.BottomSheet
import com.example.campusconnect1.presentation.components.ModernPostCard
import com.example.campusconnect1.ui.theme.PrimaryBlue
import com.example.campusconnect1.ui.theme.TextPrimary
import com.example.campusconnect1.ui.theme.TextSecondary

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
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUserData.collectAsState()
    
    // Filter States
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val currentSortType by viewModel.currentSortType.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val isGeneratingTags by viewModel.isGeneratingTags.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showUniDropdown by remember { mutableStateOf(false) }

    // Dialog States
    var showDeletePostDialog by remember { mutableStateOf<Post?>(null) }
    var showEditPostDialog by remember { mutableStateOf<Post?>(null) }
    var editPostText by remember { mutableStateOf("") }

    // --- Dialogs ---
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

    Scaffold(
        topBar = {
            // Modern Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & Campus Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showUniDropdown = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CampusFeed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUniversity,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TextSecondary
                                )
                            }
                        }
                        
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
                                    }
                                )
                            }
                        }
                    }

                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFF3F4F6), CircleShape)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FAFB)) // Light gray background
        ) {
            
            // --- Filter Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedCategory == "All") "Latest Updates" else "$selectedCategory Updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (selectedTag != null) {
                        Text(
                            text = "Filtered by: $selectedTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = { showFilterSheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filter")
                    if (selectedCategory != "All" || selectedTag != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryBlue, CircleShape)
                        )
                    }
                }
            }

            // --- Post List ---
            if (isLoading && posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No posts found", style = MaterialTheme.typography.titleMedium)
                        Text("Try changing your filters", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        TextButton(onClick = { 
                            viewModel.onCategoryFilterChange("All")
                            viewModel.onTagSelected(null)
                        }) {
                            Text("Clear all filters")
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                
                // Infinite Scroll
                val reachedBottom by remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
                    }
                }
                
                LaunchedEffect(reachedBottom) {
                    if (reachedBottom) viewModel.loadMorePosts()
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(posts) { post ->
                        val isBookmarked = currentUser?.savedPostIds?.contains(post.postId) == true
                        
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
                            onDeleteClick = { showDeletePostDialog = it },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    
                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Bottom Sheet Filter ---
    if (showFilterSheet) {
        BottomSheet(
            onDismissRequest = { showFilterSheet = false },
            title = "Filter Content"
        ) {
            // 1. Trending Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trending Tags", style = MaterialTheme.typography.titleSmall)
                TextButton(
                    onClick = { viewModel.generateTrendingTags(selectedCategory) },
                    enabled = !isGeneratingTags
                ) {
                    if (isGeneratingTags) {
                        Text("Generating...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh with AI", fontSize = 12.sp)
                    }
                }
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { viewModel.onTagSelected(null) },
                        label = { Text("All Tags") },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F2937),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.onTagSelected(if (selectedTag == tag) null else tag) },
                        label = { Text(tag) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Categories
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("All", "Academic", "News", "Event", "Confession")
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategoryFilterChange(category) },
                        label = { Text(category) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDBEAFE),
                            selectedLabelColor = Color(0xFF1D4ED8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = if (selectedCategory == category) Color.Transparent else Color(0xFFE5E7EB)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Sort Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${posts.size} results found", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row {
                        SortType.values().forEach { type ->
                            val isSelected = currentSortType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { viewModel.switchSortType(type) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type.name.lowercase().capitalize(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showFilterSheet = false },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Show Results")
            }
        }
    }
}
