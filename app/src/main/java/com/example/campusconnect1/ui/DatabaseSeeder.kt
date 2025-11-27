package com.example.campusconnect1.ui

import android.util.Log
import com.example.campusconnect1.data.model.Group
import com.example.campusconnect1.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class DatabaseSeeder {

    private val firestore = FirebaseFirestore.getInstance()

    // Daftar Kampus
    private val universities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

    // Daftar Kategori
    private val categories = listOf("General", "Academic", "Event", "Lost & Found", "Confess", "Market")

    // --- DATA DUMMY GRUP (ENGLISH) ---
    private val groupTemplates = listOf(
        "Badminton Club" to "For all badminton enthusiasts! We play every Tuesday at the sport hall.",
        "Coding Community" to "Discussing algorithms, web dev, and mobile apps. Let's code together!",
        "Esports Team" to "Recruiting players for MLBB and Valorant tournaments. Rank up with us.",
        "Secondhand Books" to "Buy and sell used textbooks cheaply. Save your money!",
        "Music Society" to "Jamming sessions and band recruitment. All genres are welcome.",
        "Photography Club" to "Capturing moments around campus. Photo hunting every weekend.",
        "English Debate" to "Sharpen your critical thinking and speaking skills."
    )

    // --- DATA DUMMY POST WITH TITLES (ENGLISH) ---
    private val postTemplates = listOf(
        Triple("Info Beasiswa LPDP 2024 - Full Scholarship!", "Does anyone know when the library closes today? I need to study for midterms! 📚", "Scholarship"),
        Triple("Selling Calculus Textbook 50% OFF", "Selling my old Calculus textbook. Good condition, 50% off! DM me if interested. 💸", "Market"),
        Triple("Found Lost Key Chain (Marvel)", "Found a lost key chain near the cafeteria. It has a Marvel logo on it. Left it at security.", "Lost & Found"),
        Triple("Looking for Gym Buddy!", "Looking for a gym buddy! Anyone want to join me at the campus gym at 5 PM?", "General"),
        Triple("New Canteen Stall Review 😋", "The food at the new canteen stall is amazing! You guys have to try the Nasi Goreng there. 😋", "General"),
        Triple("Coding Hackathon - Need Team!", "Is there any coding hackathon coming up this month? Looking for a team!", "Tech"),
        Triple("Tips Lolos Google Interview - From UI Student", "Urgent! Does anyone have the lecture notes for Intro to Business Management week 4?", "Career"),
        Triple("Sunset Photography 📸", "Sunset view from the main building today was breathtaking. 📸", "General"),
        Triple("Hackathon Season is Here! 🚀", "Reminder: The scholarship application deadline is tomorrow! Don't forget to submit.", "Event"),
        Triple("Campus Music Festival Next Week 🎸", "Who's excited for the campus music festival next week? 🎸", "Music")
    )

    // Emoji avatars for variety
    private val avatarEmojis = listOf("👨‍🎓", "👩‍🎓", "🧑‍💻", "👨‍💼", "👩‍🔬", "🧑‍🎤", "👨‍🏫", "👩‍⚕️")

    // --- GENERATE GROUPS ---
    fun seedGroups(onUpdate: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var batch = firestore.batch()
                var count = 0

                onUpdate("Generating groups...")

                universities.forEach { uniId ->
                    val selectedGroups = groupTemplates.shuffled().take(3)

                    selectedGroups.forEach { (name, desc) ->
                        val docRef = firestore.collection("groups").document()
                        val group = Group(
                            groupId = docRef.id,
                            name = "$name ($uniId)",
                            description = desc,
                            universityId = uniId,
                            creatorId = "admin_bot",
                            members = emptyList(),
                            memberCount = 0
                        )
                        batch.set(docRef, group)
                        count++
                    }
                }
                batch.commit().await()
                onUpdate("SUCCESS! Created $count groups.")
            } catch (e: Exception) {
                Log.e("Seeder", "Error groups", e)
                onUpdate("Error: ${e.message}")
            }
        }
    }

    // --- GENERATE POSTS ---
    fun seedPosts(onUpdate: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var batch = firestore.batch()
                var count = 0

                onUpdate("Generating posts...")

                universities.forEach { uniId ->
                    val selectedPosts = postTemplates.shuffled().take(5)

                    selectedPosts.forEachIndexed { index, (title, text, category) ->
                        val docRef = firestore.collection("posts").document()
                        val fakeUser = listOf("alex_student", "sarah_j", "mike_99", "campus_life", "john_doe").random()
                        val randomEmoji = avatarEmojis.random()
                        val isVerified = (0..100).random() > 70 // 30% chance verified

                        val post = Post(
                            // postId JANGAN DIISI (biar @DocumentId yang handle)
                            authorId = "dummy_user_${UUID.randomUUID()}",
                            authorName = fakeUser,
                            authorAvatar = randomEmoji,        // NEW: Emoji avatar
                            isAuthorVerified = isVerified,     // NEW: Verified badge
                            universityId = uniId,
                            title = title,                      // NEW: Post title
                            text = text,
                            imageUrl = null,
                            timestamp = Date(),
                            createdAt = System.currentTimeMillis() - (index * 3600000L), // Stagger times
                            voteCount = (0..50).random(),
                            commentCount = (0..20).random(),
                            likedBy = emptyList(),
                            category = category,
                            groupId = null,
                            groupName = null
                        )
                        batch.set(docRef, post)
                        count++
                    }
                }
                batch.commit().await()
                onUpdate("SUCCESS! Created $count posts.")
            } catch (e: Exception) {
                Log.e("Seeder", "Error posts", e)
                onUpdate("Error: ${e.message}")
            }
        }
    }
}