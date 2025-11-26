package com.example.campusconnect1.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.campusconnect1.ui.theme.AppShapes
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onPostClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.userProfile.collectAsState()
    val myPosts by viewModel.myPosts.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // State Tab
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("My Posts", "Saved")
    val tabIcons = listOf(Icons.Outlined.GridOn, Icons.Outlined.BookmarkBorder)

    var showEditDialog by remember { mutableStateOf(false) }

    // Edit Profile States
    var editBio by remember { mutableStateOf("") }
    var editMajor by remember { mutableStateOf("") }
    var editInsta by remember { mutableStateOf("") }
    var editLinked by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.updateProfilePicture(context, it) }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editMajor,
                        onValueChange = { editMajor = it },
                        label = { Text("Major") },
                        leadingIcon = { Icon(Icons.Default.School, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editInsta,
                        onValueChange = { editInsta = it },
                        label = { Text("Instagram") },
                        prefix = { Text("@") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLinked,
                        onValueChange = { editLinked = it },
                        label = { Text("LinkedIn") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfileData(editBio, editMajor, editInsta, editLinked)
                    showEditDialog = false
                }) { Text("Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        editBio = user?.bio ?: ""
                        editMajor = user?.major ?: ""
                        editInsta = user?.instagram ?: ""
                        editLinked = user?.linkedin ?: ""
                        showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, "Edit Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // --- HEADER PROFILE (LinkedIn Style) ---
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Cover Photo (Gradient Placeholder)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        )

                        // Profile Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp), // Half overlap
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (user?.profilePictureUrl.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user?.fullName?.take(1)?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(user!!.profilePictureUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Pic",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
                                // Edit Icon Overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = (-8).dp, y = (-8).dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Name & Info
                            Text(
                                text = user?.fullName ?: "Loading...",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (!user?.major.isNullOrEmpty()) {
                                Text(
                                    text = user!!.major,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${user?.universityId} • ${user?.nim}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!user?.bio.isNullOrEmpty()) {
                                Text(
                                    text = user!!.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp, vertical = 16.dp)
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Social Chips
                            if (!user?.instagram.isNullOrEmpty() || !user?.linkedin.isNullOrEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 24.dp)
                                ) {
                                    if (!user?.instagram.isNullOrEmpty()) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Instagram") },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)) } // Placeholder icon
                                        )
                                    }
                                    if (!user?.linkedin.isNullOrEmpty()) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("LinkedIn") },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)) } // Placeholder icon
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }

                // --- TABS ---
                stickyHeader {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                                icon = { Icon(tabIcons[index], null) }
                            )
                        }
                    }
                }

                // --- CONTENT ---
                if (selectedTab == 0) {
                    // MY POSTS
                    if (myPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.GridOn,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No posts yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(myPosts) { post ->
                            val isBookmarked = user?.savedPostIds?.contains(post.postId) == true
                            Box(modifier = Modifier.clickable { onPostClick(post.postId) }) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { viewModel.toggleLike(it) },
                                    onCommentClick = { onPostClick(it.postId) },
                                    isBookmarked = isBookmarked,
                                    onBookmarkClick = { viewModel.toggleBookmark(it) }
                                )
                            }
                        }
                    }
                } else {
                    // SAVED POSTS
                    if (savedPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.BookmarkBorder,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No saved posts",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(savedPosts) { post ->
                            Box(modifier = Modifier.clickable { onPostClick(post.postId) }) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { viewModel.toggleLike(it) },
                                    onCommentClick = { onPostClick(it.postId) },
                                    isBookmarked = true,
                                    onBookmarkClick = { viewModel.toggleBookmark(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}