package com.example.campusconnect1.presentation.group

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect1.data.model.Group
import com.example.campusconnect1.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GroupFeedViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data Grup (Header)
    private val _groupInfo = MutableStateFlow<Group?>(null)
    val groupInfo: StateFlow<Group?> = _groupInfo

    // Data Postingan Grup
    private val _groupPosts = MutableStateFlow<List<Post>>(emptyList())
    val groupPosts: StateFlow<List<Post>> = _groupPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadGroupData(groupId: String) {
        _isLoading.value = true

        // 1. Ambil Info Grup (Nama, Deskripsi)
        firestore.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _groupInfo.value = snapshot.toObject(Group::class.java)
                }
            }

        // 2. Ambil Postingan Khusus Grup Ini
        firestore.collection("posts")
            .whereEqualTo("groupId", groupId) // FILTER GRUP
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GroupFeed", "Error: ${error.message}")
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _groupPosts.value = snapshot.toObjects(Post::class.java)
                }
                _isLoading.value = false
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
}