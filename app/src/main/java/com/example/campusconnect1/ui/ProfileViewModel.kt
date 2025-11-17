package com.example.campusconnect1.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.Post
import com.example.campusconnect1.RetrofitClient
import com.example.campusconnect1.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileViewModel : ViewModel() {

    // ⚠️ WAJIB DIISI: API KEY IMGBB ANDA
    private val IMGBB_API_KEY = "2c12842237f145326b7757264381a895"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State Data User
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    // State List Postingan User
    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts: StateFlow<List<Post>> = _myPosts

    // State Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadUserProfile()
        loadMyPosts()
    }

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                // Ambil 1 dokumen user
                _userProfile.value = snapshot.toObject(User::class.java)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
            }
            _isLoading.value = false
        }
    }

    fun loadMyPosts() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileViewModel", "Error loading posts", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Ambil BANYAK dokumen (List) -> Gunakan .toObjects (pakai 's')
                    _myPosts.value = snapshot.toObjects(Post::class.java)
                }
            }
    }

    // Fungsi Upload dengan LOGGING LENGKAP
    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("ProfileUpload", "Mulai proses upload...")

                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    Log.d("ProfileUpload", "Gambar terbaca: ${bytes.size} bytes")

                    val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("image", "avatar.jpg", reqFile)

                    // Kirim ke ImgBB
                    val response = RetrofitClient.instance.uploadImage(IMGBB_API_KEY, body)

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("ProfileUpload", "Respon ImgBB Sukses. URL: ${responseBody?.data?.url}")

                        if (responseBody?.success == true) {
                            val newUrl = responseBody.data.url
                            val userId = auth.currentUser?.uid ?: return@launch

                            Log.d("ProfileUpload", "Mengupdate Firestore untuk User ID: $userId")

                            // Update URL di Firestore
                            firestore.collection("users").document(userId)
                                .update("profilePictureUrl", newUrl)
                                .await()

                            Log.d("ProfileUpload", "Firestore Berhasil Diupdate!")

                            // Refresh data lokal
                            loadUserProfile()
                        } else {
                            Log.e("ProfileUpload", "ImgBB response success = false")
                        }
                    } else {
                        // Jika server menolak (misal API Key salah atau gambar terlalu besar)
                        val errorBody = response.errorBody()?.string()
                        Log.e("ProfileUpload", "GAGAL UPLOAD! Code: ${response.code()}, Error: $errorBody")
                    }
                } else {
                    Log.e("ProfileUpload", "Gagal membaca file gambar (bytes null)")
                }
            } catch (e: Exception) {
                Log.e("ProfileUpload", "EXCEPTION KERAS: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }
}