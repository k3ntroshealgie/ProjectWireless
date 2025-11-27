package com.example.campusconnect1.presentation.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.data.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Daftar Grup
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 👇 STATE BARU: Apakah user ini tamu di kampus tersebut?
    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest

    // 👇 UPDATE: Terima parameter targetUniversityId
    fun loadGroups(targetUniversityId: String) {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        viewModelScope.launch {
            // 1. Cek Kampus Asli User
            val userDoc = firestore.collection("users").document(userId).get().await()
            val myUni = userDoc.getString("universityId") ?: ""

            // 2. Tentukan: Jika Target Kampus != Kampus Asli User => Tamu
            _isGuest.value = (targetUniversityId != myUni)
            Log.d("GroupViewModel", "User Uni: $myUni, Target: $targetUniversityId, IsGuest: ${_isGuest.value}")

            // 3. Ambil Grup di Kampus Target
            firestore.collection("groups")
                .whereEqualTo("universityId", targetUniversityId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("GroupViewModel", "Error loading groups", error)
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        _groups.value = snapshot.toObjects(Group::class.java)
                    }
                    _isLoading.value = false
                }
        }
    }

    fun createGroup(name: String, description: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // Ambil uni ID user (karena user hanya boleh buat grup di kampusnya sendiri)
            val userDoc = firestore.collection("users").document(userId).get().await()
            val myUni = userDoc.getString("universityId") ?: return@launch

            val newGroup = Group(
                name = name,
                description = description,
                universityId = myUni,
                creatorId = userId,
                members = listOf(userId),
                memberCount = 1
            )
            firestore.collection("groups").add(newGroup).await()
        }
    }

    fun joinGroup(groupId: String) {
        val userId = auth.currentUser?.uid ?: return
        val groupRef = firestore.collection("groups").document(groupId)

        groupRef.update("members", FieldValue.arrayUnion(userId))
        groupRef.update("memberCount", FieldValue.increment(1))
    }
}