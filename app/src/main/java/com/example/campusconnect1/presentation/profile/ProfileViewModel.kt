package com.example.campusconnect1.presentation.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User
import com.example.campusconnect1.data.remote.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
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

    private val IMGBB_API_KEY = "2c12842237f145326b7757264381a895"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts: StateFlow<List<Post>> = _myPosts

    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadUserProfileAndSavedPosts()
        loadMyPosts()
    }

    fun loadUserProfileAndSavedPosts() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("users").document(userId)
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(User::class.java)
                            _userProfile.value = user

                            val savedIds = user?.savedPostIds?.filter { it.isNotBlank() } ?: emptyList()

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
        val limitIds = ids.takeLast(10)
        // Menggunakan addSnapshotListener agar status LIKE di tab Saved juga realtime
        firestore.collection("posts")
            .whereIn(FieldPath.documentId(), limitIds)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    // Urutkan manual karena whereIn tidak menjamin urutan
                    _savedPosts.value = posts.sortedByDescending { it.timestamp }
                }
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

    // 👇 FUNGSI LIKE (Sama seperti Home)
    fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.getLong("voteCount") ?: 0
            @Suppress("UNCHECKED_CAST")
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (likedBy.contains(userId)) {
                transaction.update(postRef, "voteCount", currentLikes - 1)
                transaction.update(postRef, "likedBy", likedBy - userId)
            } else {
                transaction.update(postRef, "voteCount", currentLikes + 1)
                transaction.update(postRef, "likedBy", likedBy + userId)
            }
        }
    }

    // 👇 FUNGSI BOOKMARK (Sama seperti Home)
    fun toggleBookmark(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        val currentSaved = _userProfile.value?.savedPostIds ?: emptyList()

        if (currentSaved.contains(post.postId)) {
            userRef.update("savedPostIds", FieldValue.arrayRemove(post.postId))
        } else {
            userRef.update("savedPostIds", FieldValue.arrayUnion(post.postId))
        }
    }

    // ... (Fungsi updateProfileData & updateProfilePicture SAMA seperti sebelumnya, tidak berubah) ...
    // Agar kode tidak terlalu panjang di sini, pastikan Anda tetap menyertakan
    // updateProfileData dan updateProfilePicture yang sudah ada di file Anda sebelumnya.

    fun updateProfileData(newBio: String, newMajor: String, newInstagram: String, newGithub: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf("bio" to newBio, "major" to newMajor, "instagram" to newInstagram, "github" to newGithub)
                firestore.collection("users").document(userId).update(updates).await()
            } catch (e: Exception) { Log.e("ProfileViewModel", "Gagal update profil", e) }
        }
    }

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
                        firestore.collection("users").document(userId).update("profilePictureUrl", newUrl).await()
                    }
                }
            } catch (e: Exception) { Log.e("ProfileViewModel", "Failed to update avatar", e) }
            _isLoading.value = false
        }
    }
    fun deletePost(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        if (post.authorId == userId) {
            firestore.collection("posts").document(post.postId).delete()
        }
    }

    fun updatePost(post: Post, newText: String) {
        val userId = auth.currentUser?.uid ?: return
        if (post.authorId == userId) {
            firestore.collection("posts").document(post.postId).update("text", newText)
        }
    }
}
