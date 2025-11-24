package com.example.campusconnect1.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostSuccess: () -> Unit,
    onBack: () -> Unit,
    groupId: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // 👇 State Kategori (Default General)
    var selectedCategory by remember { mutableStateOf("General") }
    val categories = listOf("General", "Academic", "Event", "Lost & Found", "Confess", "Market")

    val postState by viewModel.postState.collectAsState()
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> imageUri = uri }

    LaunchedEffect(postState) {
        when(postState) {
            PostState.SUCCESS -> {
                Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onPostSuccess()
            }
            PostState.ERROR -> {
                Toast.makeText(context, "Failed to create post.", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupId != null) "Post to Group" else "Create Post") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {

            // 👇 UI PILIH KATEGORI
            Text("Select Category:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = (category == selectedCategory),
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        leadingIcon = if (category == selectedCategory) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = "Preview", modifier = Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
                TextButton(onClick = { imageUri = null }) { Text("Remove Image") }
            } else {
                Button(onClick = { imagePickerLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Add Image") }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // 👇 KIRIM KATEGORI
                    viewModel.createPost(context, text, imageUri, selectedCategory, groupId)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = text.isNotEmpty() && postState != PostState.LOADING
            ) {
                if (postState == PostState.LOADING) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Text("Uploading...", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Text("Post Now")
                }
            }
        }
    }
}