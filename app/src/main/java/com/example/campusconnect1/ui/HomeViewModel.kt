package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect1.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SortType { POPULAR, NEWEST }

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data Postingan
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Kampus Asli User
    private var myUniversityId: String = ""

    // Kampus yang sedang dilihat
    private val _currentViewUniversity = MutableStateFlow("Loading...")
    val currentViewUniversity: StateFlow<String> = _currentViewUniversity

    // Izin Posting
    private val _canPost = MutableStateFlow(false)
    val canPost: StateFlow<Boolean> = _canPost

    // Sorting State
    private val _currentSortType = MutableStateFlow(SortType.POPULAR)
    val currentSortType: StateFlow<SortType> = _currentSortType

    // Daftar Kampus
    val availableUniversities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

    init {
        initializeUser()
    }

    private fun initializeUser() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    myUniversityId = document.getString("universityId") ?: ""
                    switchCampus(myUniversityId)
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    // Fungsi Ganti Sortir
    fun switchSortType(sortType: SortType) {
        _currentSortType.value = sortType
        startListeningToPosts(_currentViewUniversity.value, sortType)
    }

    // Fungsi Ganti Kampus
    fun switchCampus(targetUniversity: String) {
        _currentViewUniversity.value = targetUniversity
        _canPost.value = (targetUniversity == myUniversityId)
        startListeningToPosts(targetUniversity, _currentSortType.value)
    }

    private fun startListeningToPosts(universityId: String, sortType: SortType) {
        _isLoading.value = true

        val orderByField = if (sortType == SortType.POPULAR) "voteCount" else "timestamp"
        Log.d("HomeViewModel", "Fetching $universityId sorted by $orderByField")

        firestore.collection("posts")
            .whereEqualTo("universityId", universityId)
            .orderBy(orderByField, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error listening: ${error.message}")
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.toObjects(Post::class.java)
                    _posts.value = postList
                }
                _isLoading.value = false
            }
    }

    // Fungsi Like/Unlike
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

    // 👇👇 FUNGSI DELETE (YANG TADI MISSING) 👇👇
    fun deletePost(post: Post) {
        val userId = auth.currentUser?.uid ?: return

        // Validasi pemilik
        if (post.authorId == userId) {
            firestore.collection("posts").document(post.postId)
                .delete()
                .addOnSuccessListener {
                    Log.d("HomeViewModel", "Post deleted successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("HomeViewModel", "Error deleting post", e)
                }
        }
    }

    // 👇👇 FUNGSI UPDATE (YANG TADI MISSING) 👇👇
    fun updatePost(post: Post, newText: String) {
        val userId = auth.currentUser?.uid ?: return

        if (post.authorId == userId) {
            firestore.collection("posts").document(post.postId)
                .update("text", newText)
                .addOnSuccessListener {
                    Log.d("HomeViewModel", "Post updated successfully")
                }
        }
    }

    fun fetchPosts() {
        if (_currentViewUniversity.value != "Loading...") {
            startListeningToPosts(_currentViewUniversity.value, _currentSortType.value)
        }
    }
}