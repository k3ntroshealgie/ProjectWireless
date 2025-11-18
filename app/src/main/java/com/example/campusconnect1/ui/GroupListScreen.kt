package com.example.campusconnect1.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect1.Group
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    targetUniversityId: String, // 👇 Parameter Baru
    onBack: () -> Unit,
    onGroupClick: (String) -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    // Load data saat layar dibuka
    LaunchedEffect(targetUniversityId) {
        viewModel.loadGroups(targetUniversityId)
    }

    val groups by viewModel.groups.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState() // 👇 Cek status tamu
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups @ $targetUniversityId") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            // 👇 HANYA TAMPILKAN TOMBOL CREATE JIKA BUKAN TAMU
            if (!isGuest) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Create Group")
                }
            }
        }
    ) { padding ->

        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, desc ->
                    viewModel.createGroup(name, desc)
                    showCreateDialog = false
                }
            )
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No groups found in $targetUniversityId")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(groups) { group ->
                        GroupItem(
                            group = group,
                            currentUserId = currentUserId ?: "",
                            isGuest = isGuest, // 👇 Kirim status tamu ke Item
                            onJoin = { viewModel.joinGroup(group.groupId) },
                            onClick = { onGroupClick(group.groupId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupItem(
    group: Group,
    currentUserId: String,
    isGuest: Boolean,
    onJoin: () -> Unit,
    onClick: () -> Unit
) {
    val isMember = group.members.contains(currentUserId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                Text(text = group.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${group.memberCount} members", style = MaterialTheme.typography.labelSmall)
                }
            }

            // 👇 LOGIKA TOMBOL JOIN
            if (isGuest) {
                // Jika Tamu -> Cuma bisa lihat (View Only)
                Text("View Only", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            } else if (!isMember) {
                Button(onClick = onJoin) { Text("Join") }
            } else {
                OutlinedButton(onClick = {}, enabled = false) { Text("Joined") }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, desc) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}