package com.example.campusconnect1.presentation.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.data.model.Comment
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    // ✅ FIX: Track listeners for cleanup
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // ✅ FIX: Track listener for cleanup
            val postListener = firestore.collection("posts").document(postId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("PostDetailViewModel", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val post = snapshot.toObject(Post::class.java)
                        _selectedPost.value = post
                    }
                }
            listenerRegistrations.add(postListener)
            
            // Load comments (already realtime)
            loadComments(postId)
            
            _isLoading.value = false
        }
    }

    private fun loadComments(postId: String) {
        // ✅ FIX: Track listener for cleanup
        val commentsListener = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val commentsList = snapshot.toObjects(Comment::class.java)
                    _comments.value = commentsList
                }
            }
        listenerRegistrations.add(commentsListener)
    }

    fun sendComment(postId: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            
            // Optimistic update for UI responsiveness
            val currentPost = _selectedPost.value
            if (currentPost != null) {
                _selectedPost.value = currentPost.copy(commentCount = currentPost.commentCount + 1)
            }

            // Fetch user details
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val userData = userDoc.toObject(User::class.java)
            val authorName = userData?.fullName ?: "Anonymous"

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
                // Revert optimistic update on failure
                if (currentPost != null) {
                    _selectedPost.value = currentPost
                }
            }
        }
    }

    private fun checkToxicity(text: String): Boolean {
        val badWords = listOf("bad", "hate", "stupid") 
        return badWords.any { text.contains(it, ignoreCase = true) }
    }

    fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Optimistic Update (Instant UI feedback)
        val isLiked = post.likedBy.contains(userId)
        val newLikeCount = if (isLiked) post.voteCount - 1 else post.voteCount + 1
        val newLikedBy = if (isLiked) post.likedBy - userId else post.likedBy + userId
        
        _selectedPost.value = post.copy(voteCount = newLikeCount, likedBy = newLikedBy)

        // 2. Network Request
        val postRef = firestore.collection("posts").document(post.postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            
            // ✅ FIX: Add null safety check
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
            _selectedPost.value = post
        }
    }

    fun toggleLikeComment(postId: String, comment: Comment) {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Optimistic Update
        val isLiked = comment.likedBy.contains(userId)
        val newLikeCount = if (isLiked) comment.voteCount - 1 else comment.voteCount + 1
        val newLikedBy = if (isLiked) comment.likedBy - userId else comment.likedBy + userId
        
        val updatedComment = comment.copy(voteCount = newLikeCount, likedBy = newLikedBy)
        _comments.value = _comments.value.map { if (it.commentId == comment.commentId) updatedComment else it }

        // 2. Network Request
        val commentRef = firestore.collection("posts").document(postId)
            .collection("comments").document(comment.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            
            // ✅ FIX: Add null safety check
            if (!snapshot.exists()) {
                throw IllegalStateException("Comment no longer exists")
            }
            
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
        }.addOnFailureListener {
            // Revert on failure
            _comments.value = _comments.value.map { if (it.commentId == comment.commentId) comment else it }
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
        // ✅ FIX: Optimistic update to prevent negative count
        val currentPost = _selectedPost.value
        if (currentPost != null && currentPost.commentCount > 0) {
            _selectedPost.value = currentPost.copy(commentCount = currentPost.commentCount - 1)
        }
        
        // Also remove from local comments list
        _comments.value = _comments.value.filter { it.commentId != commentId }
        
        firestore.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .delete()
            .addOnSuccessListener {
                // Decrement comment count on post
                firestore.collection("posts").document(postId)
                    .update("commentCount", FieldValue.increment(-1))
            }
            .addOnFailureListener { e ->
                // ✅ Revert on failure
                Log.e("PostDetailViewModel", "Failed to delete comment", e)
                if (currentPost != null) {
                    _selectedPost.value = currentPost
                }
                // Reload comments to restore
                loadComments(postId)
            }
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        if (newText.isBlank()) return
        firestore.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .update("text", newText)
    }

    // ✅ FIX: Cleanup listeners to prevent memory leak
    override fun onCleared() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        super.onCleared()
    }
}