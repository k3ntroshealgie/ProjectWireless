package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect1.Comment
import com.example.campusconnect1.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

class PostDetailViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadPost(postId: String) {
        _isLoading.value = true

        // 1. DENGARKAN POSTINGAN UTAMA (Agar Like & Comment Count update realtime di layar detail)
        firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val post = snapshot.toObject(Post::class.java)
                    _selectedPost.value = post
                }
            }

        // 2. DENGARKAN LIST KOMENTAR
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _comments.value = snapshot.toObjects(Comment::class.java)
                }
                _isLoading.value = false
            }
    }

    // Fungsi Like dari halaman Detail
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

    fun sendComment(postId: String, text: String) {
        if (text.isBlank()) return

        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val authorName = document.getString("fullName") ?: "Anonymous"

                val newComment = Comment(
                    postId = postId,
                    authorId = userId,
                    authorName = authorName,
                    text = text,
                    timestamp = Date()
                )

                // 1. Simpan Komentar ke Sub-collection
                firestore.collection("posts").document(postId)
                    .collection("comments")
                    .add(newComment)

                // 👇 2. PENTING: Update Angka Komentar di Postingan Utama (+1)
                firestore.collection("posts").document(postId)
                    .update("commentCount", FieldValue.increment(1))
            }
    }
}