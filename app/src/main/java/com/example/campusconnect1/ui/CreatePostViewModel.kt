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
import java.util.Date // PENTING: Untuk waktu saat ini

// Status UI
enum class PostState { IDLE, LOADING, SUCCESS, ERROR }

class CreatePostViewModel : ViewModel() {

    // ⚠️ JANGAN LUPA ISI API KEY IMGBB ANDA DI SINI
    private val IMGBB_API_KEY = "MASUKKAN_API_KEY_DISINI"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _postState = MutableStateFlow(PostState.IDLE)
    val postState: StateFlow<PostState> = _postState

    fun createPost(context: Context, text: String, imageUri: Uri?) {
        viewModelScope.launch {
            _postState.value = PostState.LOADING

            try {
                // 1. Cek Login
                val user = auth.currentUser ?: throw IllegalStateException("User belum login!")

                // 2. Ambil Data User (Cek Verified)
                val userDoc = firestore.collection("users").document(user.uid).get().await()

                val authorName = userDoc.getString("fullName") ?: "Anonymous"
                val universityId = userDoc.getString("universityId") ?: "Unknown"

                // Cek status 'verified' (sesuai field di database)
                val isAccountVerified = userDoc.getBoolean("verified") ?: false

                if (!isAccountVerified) {
                    throw IllegalStateException("Maaf, akun Anda belum terverifikasi.")
                }

                var imageUrl: String? = null

                // 3. Upload Gambar (Jika ada)
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

                // 4. Simpan Postingan
                // PERHATIKAN: Kita TIDAK mengisi 'postId' di sini.
                // Firestore akan otomatis membuatkan ID unik saat kita panggil .add()
                val newPost = Post(
                    authorId = user.uid,
                    authorName = authorName,
                    universityId = universityId,
                    text = text,
                    imageUrl = imageUrl,

                    // Gunakan waktu HP saat ini agar langsung muncul di Feed
                    timestamp = Date(),

                    voteCount = 0,
                    commentCount = 0,
                    likedBy = emptyList()
                )

                // Gunakan .add() -> Firestore generate ID otomatis -> DataModels @DocumentId akan membacanya nanti
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