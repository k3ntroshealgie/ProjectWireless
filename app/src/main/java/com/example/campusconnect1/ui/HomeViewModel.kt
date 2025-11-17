package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect1.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Enum for Sorting types
enum class SortType { POPULAR, NEWEST }

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State for Posts List
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    // State for Loading Indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // User's original University ID
    private var myUniversityId: String = ""

    // Currently selected University in the Dropdown
    private val _currentViewUniversity = MutableStateFlow("Loading...")
    val currentViewUniversity: StateFlow<String> = _currentViewUniversity

    // Permission flag: Can the user post in the current view?
    private val _canPost = MutableStateFlow(false)
    val canPost: StateFlow<Boolean> = _canPost

    // Sorting State (Default: Popular)
    private val _currentSortType = MutableStateFlow(SortType.POPULAR)
    val currentSortType: StateFlow<SortType> = _currentSortType

    // Hardcoded list for Dropdown (Ideally fetched from DB)
    val availableUniversities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

    init {
        initializeUser()
    }

    /**
     * Initial setup: Get user's university and load initial feed.
     */
    private fun initializeUser() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    myUniversityId = document.getString("universityId") ?: ""

                    // Default: Switch to user's own university
                    switchCampus(myUniversityId)
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    /**
     * Handle Like/Unlike logic using Firestore Transaction.
     * Transactions ensure data integrity when multiple users like at the same time.
     */
    fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            // Get current values safely
            val currentLikes = snapshot.getLong("voteCount") ?: 0
            @Suppress("UNCHECKED_CAST")
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (likedBy.contains(userId)) {
                // Case: UNLIKE (User already liked it)
                transaction.update(postRef, "voteCount", currentLikes - 1)
                transaction.update(postRef, "likedBy", likedBy - userId) // Remove ID from list
            } else {
                // Case: LIKE (User hasn't liked it yet)
                transaction.update(postRef, "voteCount", currentLikes + 1)
                transaction.update(postRef, "likedBy", likedBy + userId) // Add ID to list
            }
        }.addOnFailureListener { e ->
            Log.e("HomeViewModel", "Failed to toggle like", e)
        }
    }

    /**
     * Change the active Sort Type and reload data.
     */
    fun switchSortType(sortType: SortType) {
        _currentSortType.value = sortType
        // Refresh feed with new sort order
        startListeningToPosts(_currentViewUniversity.value, sortType)
    }

    /**
     * Change the active University View.
     */
    fun switchCampus(targetUniversity: String) {
        _currentViewUniversity.value = targetUniversity

        // Update permission: User can only post in their own university
        _canPost.value = (targetUniversity == myUniversityId)

        // Load posts for the target university
        startListeningToPosts(targetUniversity, _currentSortType.value)
    }

    /**
     * Real-time listener for posts based on University ID and Sort Type.
     */
    private fun startListeningToPosts(universityId: String, sortType: SortType) {
        _isLoading.value = true

        // Determine the field to sort by
        val orderByField = if (sortType == SortType.POPULAR) "voteCount" else "timestamp"

        Log.d("HomeViewModel", "Fetching $universityId sorted by $orderByField")

        firestore.collection("posts")
            .whereEqualTo("universityId", universityId)
            .orderBy(orderByField, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error listening to posts: ${error.message}")
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Convert Firestore documents to Post objects
                    val postList = snapshot.toObjects(Post::class.java)
                    _posts.value = postList
                }
                _isLoading.value = false
            }
    }

    /**
     * Manual refresh function.
     */
    fun fetchPosts() {
        if (_currentViewUniversity.value != "Loading...") {
            startListeningToPosts(_currentViewUniversity.value, _currentSortType.value)
        }
    }
}