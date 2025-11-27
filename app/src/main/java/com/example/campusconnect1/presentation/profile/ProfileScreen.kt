package com.example.campusconnect1.presentation.profile

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
import androidx.compose.material.icons.filled.*
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
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.presentation.components.ModernPostCard
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
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
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var selectedTab by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editBio by remember { mutableStateOf("") }
    var editMajor by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfilePicture(context, it) }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editMajor,
                        onValueChange = { editMajor = it },
                        label = { Text("Major") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfileData(editBio, editMajor, "", "")
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editBio = user?.bio ?: ""
                        editMajor = user?.major ?: ""
                        showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && user == null) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Gradient Header
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gradient Background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF2563EB),
                                            Color(0xFF9333EA)
                                        )
                                    )
                                )
                        )

                        // Profile Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .background(Color.White)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (user?.profilePictureUrl.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            user?.fullName?.take(1)?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(user!!.profilePictureUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Name
                            Text(
                                user?.fullName ?: "Loading...",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Major
                            if (!user?.major.isNullOrEmpty()) {
                                Text(
                                    user!!.major,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF2563EB)
                                )
                            }

                            // University
                            Text(
                                "${user?.universityId} • ${user?.nim}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Bio
                            if (!user?.bio.isNullOrEmpty()) {
                                Text(
                                    user!!.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp, vertical = 12.dp)
                                        .fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Stats Grid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Posts
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${myPosts.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Posts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Followers
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "0",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Followers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Following
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "0",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Following",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Verification Badge
                            if (user?.verified == true) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "Verified Student ✓",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                            Text(
                                                "NIM: ${user?.nim}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF16A34A).copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // Tabs
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("My Posts") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Saved") }
                        )
                    }
                }

                // Content
                if (selectedTab == 0) {
                    // My Posts
                    if (myPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No posts yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(myPosts) { post ->
                            ModernPostCard(
                                post = post,
                                currentUserId = currentUserId,
                                onCommentClick = { onPostClick(it.postId) },
                                onUpvoteClick = { viewModel.toggleLike(it) },
                                isBookmarked = user?.savedPostIds?.contains(post.postId) == true,
                                onBookmarkClick = { viewModel.toggleBookmark(it) },
                                modifier = Modifier.clickable { onPostClick(post.postId) }
                            )
                        }
                    }
                } else {
                    // Saved Posts
                    if (savedPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No saved posts",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(savedPosts) { post ->
                            ModernPostCard(
                                post = post,
                                currentUserId = currentUserId,
                                onCommentClick = { onPostClick(it.postId) },
                                onUpvoteClick = { viewModel.toggleLike(it) },
                                isBookmarked = true,
                                onBookmarkClick = { viewModel.toggleBookmark(it) },
                                modifier = Modifier.clickable { onPostClick(post.postId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
