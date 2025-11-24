package com.example.campusconnect1.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.Post
import com.example.campusconnect1.RetrofitClient
import com.example.campusconnect1.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileViewModel : ViewModel() {

    // ⚠️ PASTIKAN API KEY INI TERISI
    private val IMGBB_API_KEY = "MASUKKAN_API_KEY_DISINI"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts: StateFlow<List<Post>> = _myPosts

    // 👇 STATE BARU: Postingan yang disimpan
    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadUserProfileAndSavedPosts()
        loadMyPosts()
    }

    // Gabungkan load profil dan load saved posts agar sinkron
    fun loadUserProfileAndSavedPosts() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Ambil data User dulu (untuk dapat list ID bookmark)
                // Kita pakai Snapshot Listener agar kalau bookmark nambah, otomatis update
                firestore.collection("users").document(userId)
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(User::class.java)
                            _userProfile.value = user

                            // 2. Jika ada bookmark, ambil data postingannya
                            val savedIds = user?.savedPostIds ?: emptyList()
                            if (savedIds.isNotEmpty()) {
                                fetchSavedPosts(savedIds)
                            } else {
                                _savedPosts.value = emptyList()
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
            }
            _isLoading.value = false
        }
    }

    private fun fetchSavedPosts(ids: List<String>) {
        // Firestore limit: whereIn maksimal 10 ID. Kita ambil 10 terakhir.
        val limitIds = ids.takeLast(10)

        firestore.collection("posts")
            .whereIn(FieldPath.documentId(), limitIds)
            .get()
            .addOnSuccessListener { documents ->
                val posts = documents.toObjects(Post::class.java)
                _savedPosts.value = posts
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "Gagal ambil saved posts", it)
            }
    }

    fun loadMyPosts() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _myPosts.value = snapshot.toObjects(Post::class.java)
                }
            }
    }

    // Fungsi Update Data Text Profil
    fun updateProfileData(newBio: String, newMajor: String, newInstagram: String, newLinkedIn: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "bio" to newBio,
                    "major" to newMajor,
                    "instagram" to newInstagram,
                    "linkedin" to newLinkedIn
                )
                firestore.collection("users").document(userId).update(updates).await()
                // Tidak perlu panggil loadUserProfile lagi karena sudah pakai listener
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Gagal update profil", e)
            }
        }
    }

    // Fungsi Upload Gambar
    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("image", "avatar.jpg", reqFile)
                    val response = RetrofitClient.instance.uploadImage(IMGBB_API_KEY, body)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val newUrl = response.body()?.data?.url ?: return@launch
                        val userId = auth.currentUser?.uid ?: return@launch

                        firestore.collection("users").document(userId)
                            .update("profilePictureUrl", newUrl)
                            .await()
                        // Listener otomatis update UI
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update avatar", e)
            }
            _isLoading.value = false
        }
    }
}