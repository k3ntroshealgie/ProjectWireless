package com.example.campusconnect1

import java.util.Date // Kita pakai Date standar Java

/**
 * Model untuk koleksi 'users' di Firestore.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val universityId: String = "",
    val nim: String = "",
    // Pastikan ini sesuai dengan database Anda ('verified' atau 'isVerified')
    val verified: Boolean = false,
    val interests: List<String> = emptyList(),
    val profilePictureUrl: String = ""
)

/**
 * Model untuk koleksi 'posts' di Firestore.
 */
data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val universityId: String = "",
    val text: String = "",
    val imageUrl: String? = null,

    // 👇 UBAH DI SINI: Hapus @ServerTimestamp
    // Kita akan isi manual pakai waktu HP agar langsung muncul di list
    val timestamp: Date? = null,

    val voteCount: Int = 0,
    val commentCount: Int = 0
)

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Date? = null
)

data class University(
    val universityId: String = "",
    val name: String = "",
    val campusLogoUrl: String = ""
)

data class AllowedNIM(
    val nim: String = "",
    val universityId: String = "",
    val role: String = "student"
)