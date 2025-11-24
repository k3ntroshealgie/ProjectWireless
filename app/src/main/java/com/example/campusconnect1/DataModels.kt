package com.example.campusconnect1

import com.google.firebase.firestore.DocumentId
import java.util.Date

// 👇 UPDATE MODEL USER
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val universityId: String = "",
    val nim: String = "",
    val verified: Boolean = false,
    val profilePictureUrl: String = "",

    // Field Tambahan untuk Profil Lengkap
    val bio: String = "",           // Contoh: "Mahasiswa tingkat akhir | Hobi ngoding"
    val major: String = "",         // Contoh: "Informatika"
    val instagram: String = "",     // Username IG
    val linkedin: String = "",      // Link LinkedIn
    val interests: List<String> = emptyList(),
    val savedPostIds: List<String> = emptyList()
)

// ... (Model Post, Group, dll biarkan sama) ...
data class Post(
    @DocumentId val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val universityId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val category: String = "General",
    val timestamp: Date? = null,
    val voteCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val groupId: String? = null,
    val groupName: String? = null
)

data class Group(
    @DocumentId val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val universityId: String = "",
    val creatorId: String = "",
    val members: List<String> = emptyList(),
    val memberCount: Int = 0
)

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Date? = null
)

data class University(val universityId: String = "", val name: String = "", val campusLogoUrl: String = "")
data class AllowedNIM(val nim: String = "", val universityId: String = "", val role: String = "student")