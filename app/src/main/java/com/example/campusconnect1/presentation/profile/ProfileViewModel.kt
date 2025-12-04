package com.example.campusconnect1.presentation.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.BuildConfig
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ProfileViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    // API Key rotation support - reads from local.properties



    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Track all listener registrations for cleanup
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts: StateFlow<List<Post>> = _myPosts

    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error message state for user feedback
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Cloudinary configuration
    private val cloudinaryConfig = hashMapOf(
        "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
        "api_key" to BuildConfig.CLOUDINARY_API_KEY,
        "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
    )

    init {
        // Initialize Cloudinary
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotEmpty()) {
            try {
                MediaManager.init(getApplication(), cloudinaryConfig)
                Log.d("ProfileViewModel", "✅ Cloudinary initialized")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ Cloudinary init failed", e)
            }
        }
        
        loadUserProfileAndSavedPosts()
        loadMyPosts()
    }

    fun loadUserProfileAndSavedPosts() {
        val userId = auth.currentUser?.uid ?: return
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

                            val savedIds = user?.savedPostIds?.filter { it.isNotBlank() } ?: emptyList()

                            if (savedIds.isNotEmpty()) {
                                fetchSavedPosts(savedIds)
                            } else {
                                _savedPosts.value = emptyList()
                            }
                            _isLoading.value = false
                        } else {
                            // Document missing! Create default one.
                            Log.w("ProfileViewModel", "User document missing. Creating default...")
                            createDefaultUserDocument(userId)
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

    private fun createDefaultUserDocument(userId: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser
                val email = firebaseUser?.email ?: ""
                val name = firebaseUser?.displayName ?: "User"
                
                // Create basic user object
                val newUser = User(
                    uid = userId,
                    email = email,
                    fullName = name,
                    universityId = "ITB", // Default fallback
                    nim = "",
                    verified = false,
                    interests = emptyList()
                )
                
                firestore.collection("users").document(userId).set(newUser).await()
                Log.d("ProfileViewModel", "✅ Default user document created")
                // Listener will auto-trigger and load this new data
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to create default user", e)
                _errorMessage.value = "Gagal membuat profil default"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun fetchSavedPosts(ids: List<String>) {
        val limitIds = ids.takeLast(10)
        // Menggunakan addSnapshotListener agar status LIKE di tab Saved juga realtime
        val savedPostsListener = firestore.collection("posts")
            .whereIn(FieldPath.documentId(), limitIds)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    // Urutkan manual karena whereIn tidak menjamin urutan
                    _savedPosts.value = posts.sortedByDescending { it.timestamp }
                }
            }
        // Store listener for cleanup
        listenerRegistrations.add(savedPostsListener)
    }

    fun loadMyPosts() {
        val userId = auth.currentUser?.uid ?: return
        val myPostsListener = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _myPosts.value = snapshot.toObjects(Post::class.java)
                }
            }
        // Store listener for cleanup
        listenerRegistrations.add(myPostsListener)
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

    // Upload image to Cloudinary
    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        try {
            val context = getApplication<android.app.Application>().applicationContext
            
            // Convert URI to temp file
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val tempFile = File(context.cacheDir, "cloudinary_${System.currentTimeMillis()}.jpg")
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d("ProfileViewModel", "📤 Uploading to Cloudinary...")
            
            // Upload to Cloudinary
            MediaManager.get().upload(tempFile.absolutePath)
                .option("folder", "profile_photos")  // Your folder!
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
                        Log.d("ProfileViewModel", "✅ Success: $url")
                        tempFile.delete()
                        continuation.resume(url)
                    }
                    
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("ProfileViewModel", "❌ Error: ${error.description}")
                        tempFile.delete()
                        continuation.resume(null)
                    }
                    
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("ProfileViewModel", "⏸️ Rescheduled")
                    }
                })
                .dispatch()
                
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "💥 Exception", e)
            continuation.resume(null)
        }
    }

    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Try Cloudinary
                var imageUrl = uploadImageToCloudinary(imageUri)
                
                if (imageUrl == null) {
                     Log.e("ProfileViewModel", "Cloudinary upload failed")
                     _errorMessage.value = "Gagal mengupload foto profil"
                }
                
                // Update Firestore with new image URL
                if (imageUrl != null) {
                    val userId = auth.currentUser?.uid ?: return@launch
                    firestore.collection("users").document(userId)
                        .update("profilePictureUrl", imageUrl).await()
                    Log.d("ProfileViewModel", "✅ Profile picture updated successfully")
                } else {
                    Log.e("ProfileViewModel", "❌ Both Cloudinary and ImgBB uploads failed")
                }
            } catch (e: Exception) { 
                Log.e("ProfileViewModel", "Failed to update avatar", e) 
            }
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

    override fun onCleared() {
        // Remove all listeners when ViewModel is destroyed to prevent memory leaks
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        super.onCleared()
    }
}
