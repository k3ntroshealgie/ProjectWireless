package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.Comment
import com.example.campusconnect1.Post
import com.example.campusconnect1.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class PostDetailViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = firestore.collection("posts").document(postId).get().await()
                val post = document.toObject(Post::class.java)
                _selectedPost.value = post

                // Load comments
                loadComments(postId)
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error loading post", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadComments(postId: String) {
        firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PostDetailViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val commentsList = snapshot.toObjects(Comment::class.java)
                    _comments.value = commentsList
                }
            }
    }

    fun sendComment(postId: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                // Fetch user details for author name
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                val userData = userDoc.toObject(User::class.java)
                val authorName = userData?.fullName ?: "Anonymous"

                // Check for toxicity (Placeholder for ML integration)
                val isToxic = checkToxicity(text)
                if (isToxic) {
                    // Handle toxic comment (e.g., show error, reject)
                    Log.w("PostDetailViewModel", "Toxic comment detected: $text")
                    return@launch
                }

                val commentId = UUID.randomUUID().toString()
                val comment = Comment(
                    commentId = commentId,
                    postId = postId,
                    authorId = user.uid,
                    authorName = authorName,
                    text = text,
                    timestamp = Date()
                )

                try {
                    firestore.collection("posts").document(postId)
                        .collection("comments").document(commentId)
                        .set(comment)
                        .await()

                    // Update comment count on post
                    firestore.collection("posts").document(postId)
                        .update("commentCount", FieldValue.increment(1))

                } catch (e: Exception) {
                    Log.e("PostDetailViewModel", "Error sending comment", e)
                }
            }
        }
    }

    private fun checkToxicity(text: String): Boolean {
        // TODO: Integrate ML Kit or TensorFlow Lite for toxicity detection
        // For now, simple keyword check
        val badWords = listOf("bad", "hate", "stupid") // Example list
        return badWords.any { text.contains(it, ignoreCase = true) }
    }

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

    // --- NEW FEATURES ---

    fun toggleLikeComment(postId: String, comment: Comment) {
        val userId = auth.currentUser?.uid ?: return
        val commentRef = firestore.collection("posts").document(postId)
            .collection("comments").document(comment.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val currentLikes = snapshot.getLong("voteCount") ?: 0
            @Suppress("UNCHECKED_CAST")
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (likedBy.contains(userId)) {
                transaction.update(commentRef, "voteCount", currentLikes - 1)
                transaction.update(commentRef, "likedBy", likedBy - userId)
            } else {
                transaction.update(commentRef, "voteCount", currentLikes + 1)
                transaction.update(commentRef, "likedBy", likedBy + userId)
            }
        }
    }

    fun deletePost(postId: String, onSuccess: () -> Unit) {
        firestore.collection("posts").document(postId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("PostDetail", "Error deleting post", e) }
    }

    fun editPost(postId: String, newText: String) {
        if (newText.isBlank()) return
        firestore.collection("posts").document(postId)
            .update("text", newText)
    }

    fun deleteComment(postId: String, commentId: String) {
        firestore.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .delete()
            .addOnSuccessListener {
                // Decrement comment count on post
                firestore.collection("posts").document(postId)
                    .update("commentCount", FieldValue.increment(-1))
            }
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        if (newText.isBlank()) return
        firestore.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .update("text", newText)
    }
}