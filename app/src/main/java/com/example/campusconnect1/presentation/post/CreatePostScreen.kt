package com.example.campusconnect1.presentation.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostSuccess: () -> Unit,
    onBack: () -> Unit,
    groupId: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf("General") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }

    // User info
    var userName by remember { mutableStateOf("Loading...") }
    var userUniversity by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }

    val categories = listOf("General", "Scholarship", "Career", "Event", "Freelance", "Tech", "Sports", "Music")
    val postState by viewModel.postState.collectAsState()
    val context = LocalContext.current
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }

    // Load user info
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            userName = userDoc.getString("fullName") ?: "User"
            userUniversity = userDoc.getString("universityId") ?: ""
            isVerified = userDoc.getBoolean("verified") ?: false
        }
    }

    LaunchedEffect(postState) {
        when (val state = postState) {
            is PostState.Success -> {
                Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onPostSuccess()
            }
            is PostState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.createPost(context, title, text, imageUri, selectedCategory, groupId)
                        },
                        enabled = title.isNotEmpty() && text.isNotEmpty() && postState !is PostState.Loading
                    ) {
                        Text(
                            "POST",
                            fontWeight = FontWeight.Bold,
                            color = if (title.isNotEmpty() && text.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍🎓", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isVerified) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(0xFF2563EB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                        if (userUniversity.isNotEmpty()) {
                            Text(
                                "🎓 $userUniversity",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Category Selection
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = (category == selectedCategory),
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        leadingIcon = if (category == selectedCategory) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("What's your post about?") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Content Input
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Content") },
                placeholder = { Text("Share your thoughts...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 10
            )

            Spacer(Modifier.height(12.dp))

            // Tags Input
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("Tags") },
                placeholder = { Text("#beasiswa #LPDP") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    if (tagInput.isNotEmpty()) {
                        IconButton(onClick = {
                            tags = tags + tagInput.trim()
                            tagInput = ""
                        }) {
                            Icon(Icons.Default.Add, "Add tag")
                        }
                    }
                }
            )

            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { tags = tags.filter { it != tag } },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Image Preview or Add Button
            if (imageUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.Black)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Photo")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Post Button (Loading State)
            if (postState is PostState.Loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}