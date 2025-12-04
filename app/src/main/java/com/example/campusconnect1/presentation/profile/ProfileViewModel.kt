package com.example.campusconnect1.presentation.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect1.BuildConfig
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Track all listener registrations for cleanup
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts: StateFlow<List<Post>> = _myPosts.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val cloudinaryConfig = hashMapOf(
        "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
        "api_key" to BuildConfig.CLOUDINARY_API_KEY,
        "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
    )

    init {
        // Initialize Cloudinary if not already initialized
        try {
            MediaManager.get()
        } catch (e: Exception) {
            // Not initialized yet
            if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotEmpty()) {
                try {
                    MediaManager.init(getApplication(), cloudinaryConfig)
                    Log.d("ProfileViewModel", "✅ Cloudinary initialized")
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "❌ Cloudinary init failed", e)
                }
            }
        }
        
        loadUserProfileAndSavedPosts()
        loadMyPosts()
    }

    fun loadUserProfileAndSavedPosts() {
        val userId = auth.currentUser?.uid
        
        // ✅ FIX: Clear stale data immediately
        if (userId == null) {
            Log.e("ProfileViewModel", "No authenticated user!")
            _userProfile.value = null
            _savedPosts.value = emptyList()
            _isLoading.value = false
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userListener = firestore.collection("users").document(userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _errorMessage.value = "Gagal memuat profil: ${error.message}"
                            Log.e("ProfileViewModel", "Error loading profile", error)
                            _isLoading.value = false
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(User::class.java)
                            _userProfile.value = user
                            Log.d("ProfileViewModel", "✅ Loaded profile for: ${user?.email}")

                            val savedIds = user?.savedPostIds?.filter { it.isNotBlank() } ?: emptyList()

                            if (savedIds.isNotEmpty()) {
                                fetchSavedPosts(savedIds)
                            } else {
                                _savedPosts.value = emptyList()
                            }
                            _isLoading.value = false
                        } else {
                            // ⚠️ User document missing - should NOT happen after registration
                            Log.e("ProfileViewModel", "User document not found for $userId!")
                            _errorMessage.value = "Profile tidak ditemukan. Silahkan logout dan login kembali."
                            _isLoading.value = false
                        }
                    }
                // Store listener for cleanup
                listenerRegistrations.add(userListener)
            } catch (e: Exception) {
                _errorMessage.value = "Terjadi kesalahan saat memuat profil"
                Log.e("ProfileViewModel", "Error loading profile", e)
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadMyPosts() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val myPostsListener = firestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ProfileViewModel", "Error loading my posts", error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val posts = snapshot.toObjects(Post::class.java)
                            _myPosts.value = posts
                        }
                    }
                listenerRegistrations.add(myPostsListener)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading my posts", e)
            }
        }
    }

    private fun fetchSavedPosts(postIds: List<String>) {
        viewModelScope.launch {
            try {
                val savedPostsList = mutableListOf<Post>()
                postIds.forEach { postId ->
                    val post = firestore.collection("posts").document(postId).get().await()
                    post.toObject(Post::class.java)?.let { savedPostsList.add(it) }
                }
                _savedPosts.value = savedPostsList
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching saved posts", e)
                _savedPosts.value = emptyList()
            }
        }
    }

    fun updateProfileData(bio: String, major: String, instagram: String, github: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "bio" to bio,
                    "major" to major,
                    "instagram" to instagram,
                    "github" to github
                )
                firestore.collection("users").document(userId).update(updates).await()
                Log.d("ProfileViewModel", "✅ Profile updated")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
                _errorMessage.value = "Gagal mengupdate profil"
            }
        }
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        try {
            val context = getApplication<Application>().applicationContext

            val inputStream = context.contentResolver.openInputStream(imageUri)
            val tempFile = File(context.cacheDir, "profile_pic_${System.currentTimeMillis()}.jpg")

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("ProfileViewModel", "📤 Uploading to Cloudinary...")

            MediaManager.get().upload(tempFile.absolutePath)
                .option("folder", "profile_pictures")
                .option("resource_type", "image")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("ProfileViewModel", "⏳ Upload started")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d("ProfileViewModel", "📊 Progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        Log.d("ProfileViewModel", "✅ Upload success: $url")
                        tempFile.delete()
                        continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("ProfileViewModel", "❌ Upload error: ${error.description}")
                        tempFile.delete()
                        continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("ProfileViewModel", "⏸️ Upload rescheduled")
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            Log.e("ProfileViewModel", "💥 Exception during upload", e)
            continuation.resume(null)
        }
    }

    fun updateProfilePicture(context: Context, imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val cloudinaryUrl = uploadImageToCloudinary(imageUri)
                
                if (cloudinaryUrl != null) {
                    firestore.collection("users").document(userId)
                        .update("profilePictureUrl", cloudinaryUrl)
                        .await()
                    Log.d("ProfileViewModel", "✅ Profile picture URL updated in Firestore")
                } else {
                    _errorMessage.value = "Gagal mengupload foto profil"
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error uploading profile picture", e)
                _errorMessage.value = "Gagal mengupload foto profil"
                _isLoading.value = false
            }
        }
    }

    // ✅ FIX: Cleanup listeners AND clear state to prevent stale data
    override fun onCleared() {
        Log.d("ProfileViewModel", "🧹 Cleaning up listeners and clearing state")
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        
        // Clear all state to prevent stale data
        _userProfile.value = null
        _myPosts.value = emptyList()
        _savedPosts.value = emptyList()
        
        super.onCleared()
    }

    // ✅ Functions for post interactions in ProfileScreen
    fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        
        // Optimistic update
        val isLiked = post.likedBy.contains(userId)
        val newLikeCount = if (isLiked) post.voteCount - 1 else post.voteCount + 1
        val newLikedBy = if (isLiked) post.likedBy - userId else post.likedBy + userId
        val updatedPost = post.copy(voteCount = newLikeCount, likedBy = newLikedBy)

        // Update in myPosts
        _myPosts.value = _myPosts.value.map { if (it.postId == post.postId) updatedPost else it }
        // Update in savedPosts
        _savedPosts.value = _savedPosts.value.map { if (it.postId == post.postId) updatedPost else it }

        // Network request
        val postRef = firestore.collection("posts").document(post.postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Post no longer exists")
            }
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
        }.addOnFailureListener {
            // Revert on failure
            _myPosts.value = _myPosts.value.map { if (it.postId == post.postId) post else it }
            _savedPosts.value = _savedPosts.value.map { if (it.postId == post.postId) post else it }
        }
    }

    fun toggleBookmark(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        val currentSaved = _userProfile.value?.savedPostIds ?: emptyList()

        if (currentSaved.contains(post.postId)) {
            userRef.update("savedPostIds", com.google.firebase.firestore.FieldValue.arrayRemove(post.postId))
        } else {
            userRef.update("savedPostIds", com.google.firebase.firestore.FieldValue.arrayUnion(post.postId))
        }
    }
}

