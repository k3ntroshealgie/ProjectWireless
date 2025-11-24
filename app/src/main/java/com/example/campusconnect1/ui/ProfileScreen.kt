package com.example.campusconnect1.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.campusconnect1.ui.theme.NeoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.userProfile.collectAsState()
    val myPosts by viewModel.myPosts.collectAsState()

    // 👇 Ambil data Saved Posts
    val savedPosts by viewModel.savedPosts.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // State untuk Tab (0 = My Posts, 1 = Saved)
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("My Posts", "Saved")

    var showEditDialog by remember { mutableStateOf(false) }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editMajor, onValueChange = { editMajor = it }, label = { Text("Major (Jurusan)") })
                    OutlinedTextField(value = editBio, onValueChange = { editBio = it }, label = { Text("Bio / Status") }, maxLines = 3)
                    OutlinedTextField(value = editInsta, onValueChange = { editInsta = it }, label = { Text("Instagram Username") })
                    OutlinedTextField(value = editLinked, onValueChange = { editLinked = it }, label = { Text("LinkedIn URL") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfileData(editBio, editMajor, editInsta, editLinked)
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        editBio = user?.bio ?: ""
                        editMajor = user?.major ?: ""
                        editInsta = user?.instagram ?: ""
                        editLinked = user?.linkedin ?: ""
                        showEditDialog = true
                    }) { Icon(Icons.Default.Edit, "Edit Profile") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- HEADER ---
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(modifier = Modifier.size(120.dp).clickable { imagePicker.launch("image/*") }) {
                            if (user?.profilePictureUrl.isNullOrEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                    Text(text = user?.fullName?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.displayMedium)
                                }
                            } else {
                                AsyncImage(model = user!!.profilePictureUrl, contentDescription = "Profile Pic", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            }
                            Icon(Icons.Default.Edit, null, modifier = Modifier.align(Alignment.BottomEnd).background(NeoPrimary, CircleShape).padding(8.dp), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = user?.fullName ?: "Loading...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        if (!user?.major.isNullOrEmpty()) Text(text = user!!.major, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        Text(text = "${user?.universityId} • ${user?.nim}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (!user?.bio.isNullOrEmpty()) { Spacer(modifier = Modifier.height(4.dp)); Text(text = user!!.bio, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface) }

                        if (!user?.instagram.isNullOrEmpty() || !user?.linkedin.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!user?.instagram.isNullOrEmpty()) AssistChip(onClick = {}, label = { Text("IG") })
                                if (!user?.linkedin.isNullOrEmpty()) AssistChip(onClick = {}, label = { Text("LinkedIn") })
                            }
                        }
                    }

                    // Dark Mode Toggle
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dark Mode 🌙", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = isDarkTheme, onCheckedChange = { onThemeChange(it) })
                    }
                }

                // --- TAB SECTION ---
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- CONTENT LIST ---
                if (selectedTab == 0) {
                    // MY POSTS
                    if (myPosts.isEmpty()) {
                        item { Text("You haven't posted anything yet.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(myPosts) { post ->
                            PostCard(post = post, onLikeClick = {}, onCommentClick = {})
                        }
                    }
                } else {
                    // SAVED POSTS
                    if (savedPosts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No saved posts yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Bookmark posts to see them here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(savedPosts) { post ->
                            PostCard(post = post, onLikeClick = {}, onCommentClick = {})
                        }
                    }
                }
            }
        }
    }
}