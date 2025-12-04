package com.example.campusconnect1.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.data.model.Group
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class SortType { POPULAR, NEWEST }

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ✅ FIX: Track listeners for cleanup
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // Main Post List (Pagination supported)
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val filteredPosts: StateFlow<List<Post>> = _posts.asStateFlow()

    // Pagination State
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var isLastPage = false
    private val PAGE_SIZE = 10L

    // State Search & Filter
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData: StateFlow<User?> = _currentUserData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentViewUniversity = MutableStateFlow("Loading...")
    val currentViewUniversity: StateFlow<String> = _currentViewUniversity.asStateFlow()

    private val _canPost = MutableStateFlow(false)
    val canPost: StateFlow<Boolean> = _canPost.asStateFlow()

    private val _currentSortType = MutableStateFlow(SortType.POPULAR)
    val currentSortType: StateFlow<SortType> = _currentSortType.asStateFlow()

    private var myUniversityId: String = ""
    val availableUniversities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

    // --- Tag State ---
    private val _tags = MutableStateFlow(listOf("#BeasiswaLPDP", "#InfoMagang", "#UjianTengahSemester", "#CampusLife", "#EventCampus", "#KarirTech"))
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _isGeneratingTags = MutableStateFlow(false)
    val isGeneratingTags: StateFlow<Boolean> = _isGeneratingTags.asStateFlow()

    init {
        startListeningToUser()
    }

    // --- User & Campus Logic ---

    private fun startListeningToUser() {
        val userId = auth.currentUser?.uid ?: return
        
        // ✅ FIX: Track listener for cleanup
        val userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "User listener error", error)
                    return@addSnapshotListener
                }
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUserData.value = user

                    if (myUniversityId.isEmpty()) {
                        myUniversityId = user?.universityId ?: ""
                        Log.d("HomeViewModel", "User university: $myUniversityId")
                        switchCampus(myUniversityId)
                    }
                } else {
                    // ⚠️ User document missing - should NOT happen after registration
                    Log.e("HomeViewModel", "User document not found for $userId!")
                }
            }
        listenerRegistrations.add(userListener)
    }

    fun switchCampus(targetUniversity: String) {
        _currentViewUniversity.value = targetUniversity
        _canPost.value = (targetUniversity == myUniversityId)
        loadPosts(reset = true)
    }

    fun onCategoryFilterChange(category: String) {
        _selectedCategoryFilter.value = category
        loadPosts(reset = true)
    }

    fun switchSortType(sortType: SortType) {
        _currentSortType.value = sortType
        loadPosts(reset = true)
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        loadPosts(reset = true)
    }

    fun onTagSelected(tag: String?) {
        _selectedTag.value = tag
        loadPosts(reset = true)
    }

    fun generateTrendingTags(category: String) {
        viewModelScope.launch {
            _isGeneratingTags.value = true
            // Mock AI Generation delay
            kotlinx.coroutines.delay(1500)
            
            val newTags = when (category) {
                "Academic" -> listOf("#UjianTengahSemester", "#Skripsi", "#KRS", "#Beasiswa", "#StudyGroup")
                "News" -> listOf("#CampusNews", "#Pengumuman", "#Rektorat", "#KalenderAkademik")
                "Event" -> listOf("#EventCampus", "#Seminar", "#Workshop", "#MusicFest", "#Lomba")
                "Confession" -> listOf("#Confession", "#Curhat", "#Spotted", "#Funny", "#LoveLife")
                else -> listOf("#BeasiswaLPDP", "#InfoMagang", "#UjianTengahSemester", "#CampusLife", "#EventCampus")
            }
            _tags.value = newTags
            _isGeneratingTags.value = false
        }
    }

    // --- Pagination Logic ---

    fun loadPosts(reset: Boolean = false) {
        if (reset) {
            _posts.value = emptyList()
            lastVisibleDocument = null
            isLastPage = false
        }

        if (isLastPage && !reset) return
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val universityId = _currentViewUniversity.value
                if (universityId == "Loading...") {
                    _isLoading.value = false
                    return@launch
                }

                var query = firestore.collection("posts")
                    .whereEqualTo("universityId", universityId)

                // 1. Server-side Category Filter
                if (_selectedCategoryFilter.value != "All") {
                    query = query.whereEqualTo("category", _selectedCategoryFilter.value)
                }

                // 2. Sorting
                val orderByField = if (_currentSortType.value == SortType.POPULAR) "voteCount" else "timestamp"
                query = query.orderBy(orderByField, Query.Direction.DESCENDING)

                // 3. Pagination
                if (lastVisibleDocument != null && !reset) {
                    query = query.startAfter(lastVisibleDocument!!)
                }

                query = query.limit(PAGE_SIZE)

                val snapshot = query.get().await()

                if (!snapshot.isEmpty) {
                    val newPosts = snapshot.toObjects(Post::class.java)
                    lastVisibleDocument = snapshot.documents.lastOrNull()
                    
                    if (newPosts.size < PAGE_SIZE) {
                        isLastPage = true
                    }

                    // Client-side Search & Tag Filter
                    val searchText = _searchText.value
                    val selectedTag = _selectedTag.value

                    val filteredBatch = newPosts.filter { post ->
                        val matchesSearch = if (searchText.isNotBlank()) {
                            post.text.contains(searchText, ignoreCase = true) || 
                            post.authorName.contains(searchText, ignoreCase = true)
                        } else true

                        val matchesTag = if (selectedTag != null) {
                            val tagClean = selectedTag.removePrefix("#")
                            post.tags.any { it.contains(tagClean, ignoreCase = true) } ||
                            post.text.contains(tagClean, ignoreCase = true)
                        } else true

                        matchesSearch && matchesTag
                    }

                    if (reset) {
                        _posts.value = filteredBatch
                    } else {
                        _posts.value = _posts.value + filteredBatch
                    }
                } else {
                    isLastPage = true
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading posts", e)
            }
            _isLoading.value = false
        }
    }

    fun loadMorePosts() {
        loadPosts(reset = false)
    }

    fun refreshPosts() {
        loadPosts(reset = true)
    }

    // --- Actions (Optimistic Updates) ---

    fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Optimistic Update
        val isLiked = post.likedBy.contains(userId)
        val newLikeCount = if (isLiked) post.voteCount - 1 else post.voteCount + 1
        val newLikedBy = if (isLiked) post.likedBy - userId else post.likedBy + userId
        val updatedPost = post.copy(voteCount = newLikeCount, likedBy = newLikedBy)

        // Update list immediately
        _posts.value = _posts.value.map { if (it.postId == post.postId) updatedPost else it }

        // 2. Network Request
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
        }.addOnFailureListener {
            // Revert on failure
            _posts.value = _posts.value.map { if (it.postId == post.postId) post else it }
        }
    }

    fun toggleBookmark(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        val currentSaved = _currentUserData.value?.savedPostIds ?: emptyList()

        if (currentSaved.contains(post.postId)) {
            userRef.update("savedPostIds", FieldValue.arrayRemove(post.postId))
        } else {
            userRef.update("savedPostIds", FieldValue.arrayUnion(post.postId))
        }
    }

    fun deletePost(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        if (post.authorId == userId) {
            // Optimistic remove
            _posts.value = _posts.value.filter { it.postId != post.postId }
            
            firestore.collection("posts").document(post.postId).delete()
                .addOnFailureListener {
                    // Revert (reload)
                    loadPosts(reset = true)
                }
        }
    }

    fun updatePost(post: Post, newText: String) {
        val userId = auth.currentUser?.uid ?: return
        if (post.authorId == userId) {
            // Optimistic update
            val updatedPost = post.copy(text = newText)
            _posts.value = _posts.value.map { if (it.postId == post.postId) updatedPost else it }

            firestore.collection("posts").document(post.postId).update("text", newText)
                .addOnFailureListener {
                    // Revert
                    _posts.value = _posts.value.map { if (it.postId == post.postId) post else it }
                }
        }
    }
    
    // --- Groups & Users (Unchanged) ---
    private val _allUsersInUni = MutableStateFlow<List<User>>(emptyList())
    val allUsersInUni: StateFlow<List<User>> = _allUsersInUni.asStateFlow()

    private val _allGroupsInUni = MutableStateFlow<List<Group>>(emptyList())
    val allGroupsInUni: StateFlow<List<Group>> = _allGroupsInUni.asStateFlow()

    fun fetchAllUsersAndGroups(universityId: String) {
        firestore.collection("users").whereEqualTo("universityId", universityId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) _allUsersInUni.value = snapshot.toObjects(User::class.java)
            }
        firestore.collection("groups").whereEqualTo("universityId", universityId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) _allGroupsInUni.value = snapshot.toObjects(Group::class.java)
            }
    }

    // ✅ FIX: Cleanup listeners to prevent memory leak
    override fun onCleared() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        super.onCleared()
    }
}