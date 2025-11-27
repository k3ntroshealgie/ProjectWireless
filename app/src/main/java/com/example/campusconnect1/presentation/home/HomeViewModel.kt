package com.example.campusconnect1.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.data.model.Group
import com.example.campusconnect1.data.model.Post
import com.example.campusconnect1.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

    // Data Mentah Postingan (Belum difilter search/kategori)
    private val _rawPosts = MutableStateFlow<List<Post>>(emptyList())

    // State Pencarian Teks
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    // State Filter Kategori (All, Academic, dll)
    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter

    // State User (Untuk mengetahui bookmark & kampus asli)
    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData: StateFlow<User?> = _currentUserData

    // 👇 LOGIKA FILTER GABUNGAN (Search + Kategori)
    // Ini otomatis update jika rawPosts, searchText, atau category berubah
    val filteredPosts: StateFlow<List<Post>> = combine(
        _searchText,
        _rawPosts,
        _selectedCategoryFilter
    ) { text, posts, category ->
        var result = posts

        // 1. Filter Kategori
        if (category != "All") {
            result = result.filter { it.category == category }
        }

        // 2. Filter Search Text (Nama atau Isi Postingan)
        if (text.isNotBlank()) {
            result = result.filter {
                it.text.contains(text, ignoreCase = true) ||
                        it.authorName.contains(text, ignoreCase = true)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Kampus yang sedang dilihat
    private val _currentViewUniversity = MutableStateFlow("Loading...")
    val currentViewUniversity: StateFlow<String> = _currentViewUniversity

    // Izin Posting (Hanya true jika di kampus sendiri)
    private val _canPost = MutableStateFlow(false)
    val canPost: StateFlow<Boolean> = _canPost

    // Sorting (Populer / Terbaru)
    private val _currentSortType = MutableStateFlow(SortType.POPULAR)
    val currentSortType: StateFlow<SortType> = _currentSortType

    private var myUniversityId: String = ""
    val availableUniversities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

    init {
        // Mulai memantau data user saat ViewModel dibuat
        startListeningToUser()
    }

    // Input Search & Kategori
    fun onSearchTextChange(text: String) { _searchText.value = text }
    fun onCategoryFilterChange(category: String) { _selectedCategoryFilter.value = category }

    // 👇 Memantau Data User (Bookmark & Kampus Asli)
    private fun startListeningToUser() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        firestore.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error listening user", error)
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUserData.value = user

                    // Set kampus awal jika belum diset
                    if (myUniversityId.isEmpty()) {
                        myUniversityId = user?.universityId ?: ""
                        switchCampus(myUniversityId)
                    }
                }
                _isLoading.value = false
            }
    }

    // Ganti Kampus (Dropdown)
    fun switchCampus(targetUniversity: String) {
        _currentViewUniversity.value = targetUniversity
        _canPost.value = (targetUniversity == myUniversityId)
        startListeningToPosts(targetUniversity, _currentSortType.value)
    }

    // Ganti Sortir (Chip)
    fun switchSortType(sortType: SortType) {
        _currentSortType.value = sortType
        startListeningToPosts(_currentViewUniversity.value, sortType)
    }

    // Ambil Postingan dari Firestore (Real-time)
    private fun startListeningToPosts(universityId: String, sortType: SortType) {
        _isLoading.value = true
        val orderByField = if (sortType == SortType.POPULAR) "voteCount" else "timestamp"

        firestore.collection("posts")
            .whereEqualTo("universityId", universityId)
            .orderBy(orderByField, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error listening posts", error)
                    _isLoading.value = false;
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _rawPosts.value = snapshot.toObjects(Post::class.java)
                }
                _isLoading.value = false
            }
    }

    // 👇 Fitur Bookmark (Simpan Postingan)
    fun toggleBookmark(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        // Cek apakah sudah ada di list lokal
        val currentSaved = _currentUserData.value?.savedPostIds ?: emptyList()

        if (currentSaved.contains(post.postId)) {
            // Hapus
            userRef.update("savedPostIds", FieldValue.arrayRemove(post.postId))
        } else {
            // Tambah (Otomatis buat field jika belum ada)
            userRef.update("savedPostIds", FieldValue.arrayUnion(post.postId))
        }
    }

    // Fitur Like / Unlike
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

    // Hapus Postingan
    fun deletePost(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        if (post.authorId == userId) {
            firestore.collection("posts").document(post.postId).delete()
        }
    }

    // Edit Postingan
    fun updatePost(post: Post, newText: String) {
        val userId = auth.currentUser?.uid ?: return
        if (post.authorId == userId) {
            firestore.collection("posts").document(post.postId).update("text", newText)
        }
    }

    // Refresh Manual
    fun fetchPosts() {
        if (_currentViewUniversity.value != "Loading...") {
            startListeningToPosts(_currentViewUniversity.value, _currentSortType.value)
        }
    }

    private val _allUsersInUni = MutableStateFlow<List<User>>(emptyList())
    val allUsersInUni: StateFlow<List<User>> = _allUsersInUni

    private val _allGroupsInUni = MutableStateFlow<List<Group>>(emptyList())
    val allGroupsInUni: StateFlow<List<Group>> = _allGroupsInUni

    // Dipanggil saat Home/ViewModel dibuat
    fun fetchAllUsersAndGroups(universityId: String) {
        // Ambil Semua User
        firestore.collection("users")
            .whereEqualTo("universityId", universityId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _allUsersInUni.value = snapshot.toObjects(User::class.java)
                }
            }

        // Ambil Semua Grup
        firestore.collection("groups")
            .whereEqualTo("universityId", universityId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _allGroupsInUni.value = snapshot.toObjects(Group::class.java)
                }
            }
    }
}