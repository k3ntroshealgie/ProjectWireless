package com.example.campusconnect1.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * 🎯 TODO: Ganti dengan API Key Anda!
 * Daftar di https://api.imgbb.com/ untuk mendapatkan API key gratis.
 */
const val IMGBB_API_KEY = "2b9cc62a20ca0f2e9ca6e8617e545219"

data class ImgbbResponse(
    val data: ImgbbData,
    val success: Boolean
)

data class ImgbbData(
    val url: String // Ini adalah URL gambar yang akan kita simpan ke Firestore
)

interface ImgbbApiService {

    /**
     * Meng-upload gambar ke server imgbb.
     * @param apiKey Kunci API Anda.
     * @param image File gambar yang di-upload sebagai MultipartBody.Part.
     * @param name Nama file (opsional, tapi disarankan).
     */
    @Multipart
    @POST("1/upload") // Endpoint untuk upload
    suspend fun uploadImage(
        @Query("key") apiKey: String,
        @Part image: MultipartBody.Part,
        @Part("name") name: RequestBody = RequestBody.Companion.create(null, "image")
    ): Response<ImgbbResponse> // Menggunakan Response<T> untuk error handling
}

object RetrofitClient {
    private const val BASE_URL = "https://api.imgbb.com/"

    val instance: ImgbbApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Kita butuh 'converter-gson' untuk mengubah JSON respons
            // menjadi data class (ImgbbResponse)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ImgbbApiService::class.java)
    }
}