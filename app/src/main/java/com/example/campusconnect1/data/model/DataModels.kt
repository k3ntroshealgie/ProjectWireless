package com.example.campusconnect1.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

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
    val github: String = "",        // Username GitHub
    val interests: List<String> = emptyList(),
    val savedPostIds: List<String> = emptyList()
)

data class Post(
    @DocumentId val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "👤",        // Emoji avatar for modern UI
    val authorAvatarUrl: String = "",       // Keep for backward compatibility
    val universityId: String = "",
    var authorVerified: Boolean = false,    // Verified badge (Renamed from isAuthorVerified to match Firestore)
    val title: String = "",                 // Post title (bold header)
    val text: String = "",
    val imageUrl: String? = null,
    val category: String = "General",
    val timestamp: Date? = null,
    val createdAt: Long = System.currentTimeMillis(), // For time formatting
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
    @DocumentId val id: String = "",
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Date? = null,
    val voteCount: Int = 0,
    val likedBy: List<String> = emptyList()
)

data class University(val universityId: String = "", val name: String = "", val campusLogoUrl: String = "")
data class AllowedNIM(val nim: String = "", val universityId: String = "", val role: String = "student")