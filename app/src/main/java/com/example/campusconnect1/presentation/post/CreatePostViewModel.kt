package com.example.campusconnect1.presentation.post

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
import com.example.campusconnect1.ml.TextClassifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed interface PostState {
    object Idle : PostState
    object Loading : PostState
    object Success : PostState
    data class Error(val message: String) : PostState
}

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _postState = MutableStateFlow<PostState>(PostState.Idle)
    val postState: StateFlow<PostState> = _postState

    // Cloudinary configuration
    private val cloudinaryConfig = hashMapOf(
        "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
        "api_key" to BuildConfig.CLOUDINARY_API_KEY,
        "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
    )

    init {
        // Initialize Cloudinary if not already initialized
        try {
            MediaManager.get() // Check if initialized
        } catch (e: Exception) {
            if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotEmpty()) {
                try {
                    MediaManager.init(getApplication(), cloudinaryConfig)
                    Log.d("CreatePostViewModel", "✅ Cloudinary initialized")
                } catch (e: Exception) {
                    Log.e("CreatePostViewModel", "❌ Cloudinary init failed", e)
                }
            }
        }
    }

    // Upload image to Cloudinary
    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        try {
            val context = getApplication<Application>().applicationContext
            
            // Convert URI to temp file
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val tempFile = File(context.cacheDir, "post_${System.currentTimeMillis()}.jpg")
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d("CreatePostViewModel", "📤 Uploading to Cloudinary...")
            
            // Upload to Cloudinary
            MediaManager.get().upload(tempFile.absolutePath)
                .option("folder", "posts_images")  // Use posts_images folder
                .option("resource_type", "image")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("CreatePostViewModel", "⏳ Upload started")
                    }
                    
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d("CreatePostViewModel", "📊 Progress: $progress%")
                    }
                    
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        Log.d("CreatePostViewModel", "✅ Success: $url")
                        tempFile.delete()
                        continuation.resume(url)
                    }
                    
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("CreatePostViewModel", "❌ Error: ${error.description}")
                        tempFile.delete()
                        continuation.resume(null)
                    }
                    
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("CreatePostViewModel", "⏸️ Rescheduled")
                    }
                })
                .dispatch()
                
        } catch (e: Exception) {
            Log.e("CreatePostViewModel", "💥 Exception", e)
            continuation.resume(null)
        }
    }

    // ✅ FIX: Add tags parameter
    fun createPost(
        context: Context,
        title: String,
        text: String,
        imageUri: Uri?,
        category: String,
        groupId: String? = null,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _postState.value = PostState.Loading
            try {
                // 1. ML CHECK: Deteksi konten toxic
                val classification = TextClassifier.classify(text)
                if (classification.isToxic) {
                    Log.w("CreatePost", "Toxic content detected: ${classification.confidence}")
                    throw SecurityException("Post rejected: Toxic content detected.")
                }

                val user = auth.currentUser ?: throw IllegalStateException("User not logged in!")
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                val authorName = userDoc.getString("fullName") ?: "Anonymous"
                val universityId = userDoc.getString("universityId") ?: "Unknown"
                val authorAvatarUrl = userDoc.getString("profilePictureUrl") ?: ""
                val authorAvatar = "👨‍🎓" // TODO: Get from userDoc when User model is updated
                val isAuthorVerified = userDoc.getBoolean("verified") ?: false

                val isAccountVerified = userDoc.getBoolean("verified") ?: false
                if (!isAccountVerified) throw IllegalStateException("Account not verified.")

                var imageUrl: String? = null
                if (imageUri != null) {
                    // Try Cloudinary first
                    imageUrl = uploadImageToCloudinary(imageUri)
                }

                val newPost = Post(
                    authorId = user.uid,
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    authorAvatarUrl = authorAvatarUrl,
                    authorVerified = isAuthorVerified,
                    universityId = universityId,
                    title = title,
                    text = text,
                    imageUrl = imageUrl,
                    category = category,
                    timestamp = Date(),
                    createdAt = System.currentTimeMillis(),
                    voteCount = 0,
                    commentCount = 0,
                    likedBy = emptyList(),
                    groupId = groupId,
                    tags = tags // ✅ FIX: Save tags to Firestore
                )

                firestore.collection("posts").add(newPost).await()
                _postState.value = PostState.Success
            } catch (e: SecurityException) {
                Log.e("CreatePost", "Toxic Error", e)
                _postState.value = PostState.Error(e.message ?: "Content contains toxic words.")
            } catch (e: Exception) {
                Log.e("CreatePost", "Error", e)
                _postState.value = PostState.Error("Failed to create post. Check connection.")
            }
        }
    }
    fun resetState() { _postState.value = PostState.Idle }
}