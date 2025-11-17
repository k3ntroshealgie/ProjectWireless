package com.example.campusconnect1

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Model User
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val universityId: String = "",
    val nim: String = "",
    val verified: Boolean = false,
    val interests: List<String> = emptyList(),
    val profilePictureUrl: String = ""
)

/**
 * Model Post (Updated)
 */
data class Post(
    @DocumentId
    val postId: String = "",

    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val universityId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Date? = null,
    val voteCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList(),

    // 👇 TAMBAHAN BARU UNTUK GRUP
    val groupId: String? = null,   // Null = Postingan Home biasa
    val groupName: String? = null
)

/**
 * Model Group (Baru)
 */
data class Group(
    @DocumentId
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val universityId: String = "",  // Grup milik kampus mana
    val creatorId: String = "",
    val members: List<String> = emptyList(), // List UID member
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