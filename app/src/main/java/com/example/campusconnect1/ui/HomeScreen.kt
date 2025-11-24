package com.example.campusconnect1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.Post
import com.example.campusconnect1.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onFabClick: () -> Unit,
    onLogout: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onUniversityChange: (String) -> Unit
) {
    val posts by homeViewModel.filteredPosts.collectAsState()
    val searchText by homeViewModel.searchText.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val currentUni by homeViewModel.currentViewUniversity.collectAsState()
    val canPost by homeViewModel.canPost.collectAsState()
    val currentSort by homeViewModel.currentSortType.collectAsState()
    val universities = homeViewModel.availableUniversities
    val currentUser by homeViewModel.currentUserData.collectAsState()

    val selectedCategory by homeViewModel.selectedCategoryFilter.collectAsState()
    val categories = listOf("All", "General", "Academic", "Event", "Lost & Found", "Confess", "Market")

    var expanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var editTextField by remember { mutableStateOf("") }

    LaunchedEffect(currentUni) {
        if (currentUni != "Loading...") {
            onUniversityChange(currentUni)
        }
    }

    if (postToEdit != null) {
        AlertDialog(
            onDismissRequest = { postToEdit = null },
            containerColor = NeoCard,
            titleContentColor = NeoText,
            textContentColor = NeoText,
            title = { Text("Edit Post") },
            text = {
                OutlinedTextField(
                    value = editTextField,
                    onValueChange = { editTextField = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeoPrimary,
                        unfocusedBorderColor = NeoTextLight,
                        focusedTextColor = NeoText,
                        unfocusedTextColor = NeoText
                    )
                )
            },
            confirmButton = {
                Button(onClick = { homeViewModel.updatePost(postToEdit!!, editTextField); postToEdit = null }, colors = ButtonDefaults.buttonColors(containerColor = NeoPrimary)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { postToEdit = null }) { Text("Cancel", color = NeoTextLight) } }
        )
    }

    Scaffold(
        containerColor = NeoBackground,
        topBar = {
            Column {
                if (isSearching) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchText,
                                onValueChange = { homeViewModel.onSearchTextChange(it) },
                                placeholder = { Text("Search...", color = NeoTextLight) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = NeoText,
                                    unfocusedTextColor = NeoText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = { IconButton(onClick = { isSearching = false; homeViewModel.onSearchTextChange("") }) { Icon(Icons.Default.ArrowBack, "Back", tint = NeoText) } },
                        actions = { if (searchText.isNotEmpty()) IconButton(onClick = { homeViewModel.onSearchTextChange("") }) { Icon(Icons.Default.Close, "Clear", tint = NeoText) } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = NeoCard)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { expanded = true }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = currentUni,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeoText,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, "Switch", tint = NeoSecondary)

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(NeoCard)
                                ) {
                                    universities.forEach { uni ->
                                        DropdownMenuItem(text = { Text(uni, color = NeoText) }, onClick = { homeViewModel.switchCampus(uni); expanded = false })
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = NeoCard),
                        actions = {
                            IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, "Search", tint = NeoText) }
                            IconButton(onClick = onProfileClick) { Icon(Icons.Default.Person, "Profile", tint = NeoText) }
                            IconButton(onClick = { FirebaseAuth.getInstance().signOut(); onLogout() }) { Icon(Icons.Default.ExitToApp, "Logout", tint = Color.Red) }
                        }
                    )
                }

                if (!isSearching) {
                    Column(modifier = Modifier.background(NeoCard)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = currentSort == SortType.POPULAR,
                                onClick = { homeViewModel.switchSortType(SortType.POPULAR) },
                                label = { Text("Popular") },
                                leadingIcon = if (currentSort == SortType.POPULAR) { { Icon(Icons.Default.Favorite, null, Modifier.size(18.dp)) } } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeoAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = Color.Black,
                                    selectedLeadingIconColor = NeoPrimary
                                )
                            )
                            FilterChip(
                                selected = currentSort == SortType.NEWEST,
                                onClick = { homeViewModel.switchSortType(SortType.NEWEST) },
                                label = { Text("Newest") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeoBackground, selectedLabelColor = NeoText)
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { homeViewModel.onCategoryFilterChange(cat) },
                                    label = { Text(cat) },
                                    leadingIcon = if (selectedCategory == cat) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeoSecondary.copy(alpha = 0.2f),
                                        selectedLabelColor = NeoPrimary
                                    )
                                )
                            }
                        }
                        Divider(color = NeoTextLight.copy(alpha = 0.1f))
                    }
                }
            }
        },
        floatingActionButton = {
            if (canPost && !isSearching) {
                FloatingActionButton(onClick = onFabClick, containerColor = NeoPrimary, contentColor = Color.White) {
                    Icon(Icons.Default.Add, "New Post")
                }
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeoPrimary)
            } else {
                if (posts.isEmpty()) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (searchText.isNotEmpty()) "No results for '$searchText'" else "No posts yet in $currentUni.", color = NeoTextLight)
                        if (canPost) Text("Be the first to post!", color = NeoPrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // 👇 BAGIAN TERPENTING
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(posts) { post ->
                            val isBookmarked = currentUser?.savedPostIds?.contains(post.postId) == true

                            Box(modifier = Modifier.clickable { onPostClick(post.postId) }) {
                                PostCard(
                                    post = post,
                                    // 👇 KABEL UTAMA LIKE
                                    onLikeClick = { selectedPost ->
                                        homeViewModel.toggleLike(selectedPost)
                                    },
                                    onCommentClick = { onPostClick(it.postId) },
                                    onDeleteClick = { homeViewModel.deletePost(it) },
                                    onEditClick = { editTextField = it.text; postToEdit = it },
                                    isBookmarked = isBookmarked,
                                    onBookmarkClick = { homeViewModel.toggleBookmark(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}