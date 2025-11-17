package com.example.campusconnect1.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect1.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // State untuk menyimpan daftar postingan
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    // State untuk loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Langsung pasang "CCTV" (Listener) saat ViewModel dibuat
        startListeningToPosts()
    }

    // Fungsi ini akan memantau database secara Live
    private fun startListeningToPosts() {
        _isLoading.value = true

        // Menggunakan addSnapshotListener (Real-time)
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Urutkan dari yang terbaru
            .addSnapshotListener { snapshot, error ->

                // Jika terjadi error koneksi, hentikan loading dan log error
                if (error != null) {
                    Log.e("HomeViewModel", "Error listening to posts", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                // Jika ada data baru, update _posts
                if (snapshot != null) {
                    val postList = snapshot.toObjects(Post::class.java)
                    _posts.value = postList

                    Log.d("HomeViewModel", "Real-time update: ${postList.size} posts")
                }

                _isLoading.value = false
            }
    }

    // Fungsi manual refresh (opsional, karena sudah real-time)
    fun fetchPosts() {
        // Kita bisa kosongkan ini atau biarkan me-restart listener jika perlu
        // Tapi dengan snapshot listener, tombol refresh sebenarnya tidak terlalu dibutuhkan lagi
    }
}