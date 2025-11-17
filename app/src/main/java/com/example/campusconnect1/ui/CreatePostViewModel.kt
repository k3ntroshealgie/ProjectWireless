package com.example.campusconnect1.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.Post
import com.example.campusconnect1.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Date

enum class PostState { IDLE, LOADING, SUCCESS, ERROR }

class CreatePostViewModel : ViewModel() {

    // ⚠️ JANGAN LUPA ISI API KEY IMGBB ANDA DI SINI
    private val IMGBB_API_KEY = "2c12842237f145326b7757264381a895"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _postState = MutableStateFlow(PostState.IDLE)
    val postState: StateFlow<PostState> = _postState

    // 👇 UPDATE: Tambahkan parameter groupId (Default null)
    fun createPost(context: Context, text: String, imageUri: Uri?, groupId: String? = null) {
        viewModelScope.launch {
            _postState.value = PostState.LOADING

            try {
                val user = auth.currentUser ?: throw IllegalStateException("User belum login!")

                // Ambil Data User
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                val authorName = userDoc.getString("fullName") ?: "Anonymous"
                val universityId = userDoc.getString("universityId") ?: "Unknown"
                val authorAvatarUrl = userDoc.getString("profilePictureUrl") ?: ""

                // Cek status verified
                val isAccountVerified = userDoc.getBoolean("verified") ?: false
                if (!isAccountVerified) {
                    throw IllegalStateException("Maaf, akun Anda belum terverifikasi.")
                }

                var imageUrl: String? = null

                // Upload Gambar (Jika ada)
                if (imageUri != null) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("image", "upload.jpg", reqFile)

                        val response = RetrofitClient.instance.uploadImage(IMGBB_API_KEY, body)

                        if (response.isSuccessful && response.body()?.success == true) {
                            imageUrl = response.body()?.data?.url
                        } else {
                            throw Exception("Gagal upload gambar ke imgbb")
                        }
                    }
                }

                // Simpan Postingan
                val newPost = Post(
                    authorId = user.uid,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatarUrl,
                    universityId = universityId,
                    text = text,
                    imageUrl = imageUrl,
                    timestamp = Date(),
                    voteCount = 0,
                    commentCount = 0,
                    likedBy = emptyList(),

                    // 👇 ISI GROUP ID DI SINI (Bisa null jika postingan biasa)
                    groupId = groupId
                )

                firestore.collection("posts").add(newPost).await()

                _postState.value = PostState.SUCCESS

            } catch (e: Exception) {
                Log.e("CreatePost", "Error creating post", e)
                _postState.value = PostState.ERROR
            }
        }
    }

    fun resetState() {
        _postState.value = PostState.IDLE
    }
}