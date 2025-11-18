package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect1.Comment
import com.example.campusconnect1.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

class PostDetailViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Holds the list of comments for the specific post
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    // Holds the Post data itself (to display at the top)
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Load post details and start listening to its comments.
     * Called when the screen opens.
     */
    fun loadPost(postId: String) {
        _isLoading.value = true

        // 1. Get the Post Document (Real-time updates for like counts etc)
        firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _selectedPost.value = snapshot.toObject(Post::class.java)
                }
            }

        // 2. Listen to Comments Sub-collection
        // Structure: collection("posts") -> doc(postId) -> collection("comments")
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Oldest comments at top
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostDetail", "Error fetching comments", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val commentList = snapshot.toObjects(Comment::class.java)
                    _comments.value = commentList
                }
                _isLoading.value = false
            }
    }

    /**
     * Send a new comment.
     */
    fun sendComment(postId: String, text: String) {
        if (text.isBlank()) return

        val userId = auth.currentUser?.uid ?: return

        // Get user info first (to display name in comment)
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val authorName = document.getString("fullName") ?: "Anonymous"

                val newComment = Comment(
                    postId = postId,
                    authorId = userId,
                    authorName = authorName,
                    text = text,
                    timestamp = Date() // Client time
                )

                // Add to 'comments' sub-collection
                firestore.collection("posts").document(postId)
                    .collection("comments")
                    .add(newComment)

                // Update commentCount in the main Post document
                // Note: Ideally use a Transaction or Cloud Function for accuracy
                firestore.collection("posts").document(postId)
                    .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
            }
    }
}