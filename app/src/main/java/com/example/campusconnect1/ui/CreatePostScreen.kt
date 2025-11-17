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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostSuccess: () -> Unit, // Callback saat sukses (untuk kembali ke Home)
    onBack: () -> Unit,        // Callback tombol kembali
    viewModel: CreatePostViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val postState by viewModel.postState.collectAsState()
    val context = LocalContext.current

    // Launcher untuk memilih gambar dari Galeri
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // Efek Samping untuk Toast & Navigasi
    LaunchedEffect(postState) {
        when(postState) {
            PostState.SUCCESS -> {
                Toast.makeText(context, "Postingan berhasil dibuat!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onPostSuccess() // Kembali ke Home
            }
            PostState.ERROR -> {
                Toast.makeText(context, "Gagal membuat postingan.", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat Postingan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
            // Input Teks
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Apa yang Anda pikirkan?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Pilih Gambar & Preview
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                TextButton(onClick = { imageUri = null }) {
                    Text("Hapus Gambar")
                }
            } else {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Tambahkan Gambar")
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Dorong tombol ke bawah

            // Tombol Posting
            Button(
                onClick = { viewModel.createPost(context, text, imageUri) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = text.isNotEmpty() && postState != PostState.LOADING
            ) {
                if (postState == PostState.LOADING) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mengupload...")
                } else {
                    Text("Posting Sekarang")
                }
            }
        }
    }
}