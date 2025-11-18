package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SortType { POPULAR, NEWEST }

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data Mentah dari Database
    private val _rawPosts = MutableStateFlow<List<Post>>(emptyList())

    // Text Pencarian
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    // 👇 LOGIKA FILTER PINTAR:
    // Menggabungkan Data Mentah + Text Search -> Menghasilkan Data Tersaring
    val filteredPosts: StateFlow<List<Post>> = _searchText
        .combine(_rawPosts) { text, posts ->
            if (text.isBlank()) {
                posts // Jika search kosong, tampilkan semua
            } else {
                // Filter postingan yang isinya mengandung text pencarian (ignore case)
                posts.filter { post ->
                    post.text.contains(text, ignoreCase = true) ||
                            post.authorName.contains(text, ignoreCase = true)
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentViewUniversity = MutableStateFlow("Loading...")
    val currentViewUniversity: StateFlow<String> = _currentViewUniversity

    private val _canPost = MutableStateFlow(false)
    val canPost: StateFlow<Boolean> = _canPost

    private val _currentSortType = MutableStateFlow(SortType.POPULAR)
    val currentSortType: StateFlow<SortType> = _currentSortType

    private var myUniversityId: String = ""
    val availableUniversities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

    init {
        initializeUser()
    }

    // Update text search saat user mengetik
    fun onSearchTextChange(text: String) {
        _searchText.value = text
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
            .addOnFailureListener { _isLoading.value = false }
    }

    fun switchSortType(sortType: SortType) {
        _currentSortType.value = sortType
        startListeningToPosts(_currentViewUniversity.value, sortType)
    }

    fun switchCampus(targetUniversity: String) {
        _currentViewUniversity.value = targetUniversity
        _canPost.value = (targetUniversity == myUniversityId)
        startListeningToPosts(targetUniversity, _currentSortType.value)
    }

    private fun startListeningToPosts(universityId: String, sortType: SortType) {
        _isLoading.value = true

        val orderByField = if (sortType == SortType.POPULAR) "voteCount" else "timestamp"

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
                    // Simpan ke _rawPosts (Data Mentah)
                    _rawPosts.value = snapshot.toObjects(Post::class.java)
                }
                _isLoading.value = false
            }
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

    fun fetchPosts() {
        if (_currentViewUniversity.value != "Loading...") {
            startListeningToPosts(_currentViewUniversity.value, _currentSortType.value)
        }
    }
}