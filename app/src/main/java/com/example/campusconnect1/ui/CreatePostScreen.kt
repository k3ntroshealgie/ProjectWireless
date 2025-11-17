package com.example.campusconnect1.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    // 👇 UPDATE: Receive groupId parameter
    groupId: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val postState by viewModel.postState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(postState) {
        when(postState) {
            PostState.SUCCESS -> {
                // Translated to English
                Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onPostSuccess()
            }
            PostState.ERROR -> {
                // Translated to English
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Translated contentDescription
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                // Translated label
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
                TextButton(onClick = { imageUri = null }) {
                    // Translated button text
                    Text("Remove Image")
                }
            } else {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    // Translated button text
                    Text("Add Image")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // 👇 UPDATE: Send groupId to ViewModel
                    viewModel.createPost(context, text, imageUri, groupId)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = text.isNotEmpty() && postState != PostState.LOADING
            ) {
                if (postState == PostState.LOADING) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    // Translated loading text
                    Text("Uploading...")
                } else {
                    // Translated button text
                    Text("Post Now")
                }
            }
        }
    }
}