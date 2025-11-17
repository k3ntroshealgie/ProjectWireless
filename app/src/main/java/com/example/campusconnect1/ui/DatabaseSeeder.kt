package com.example.campusconnect1.ui

import android.util.Log
import com.example.campusconnect1.Group
import com.example.campusconnect1.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class DatabaseSeeder {

    private val firestore = FirebaseFirestore.getInstance()

    // Daftar Kampus (Harus sama dengan yang ada di HomeViewModel)
    private val universities = listOf("ITB", "UI", "UGM", "ITS", "IPB", "UNAIR", "UNDIP", "UNPAD", "TELKOMU", "PU", "UNSRI")

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

    // --- DATA DUMMY POST (ENGLISH) ---
    private val postTemplates = listOf(
        "Does anyone know when the library closes today? I need to study for midterms! 📚",
        "Selling my old Calculus textbook. Good condition, 50% off! DM me if interested. 💸",
        "Found a lost key chain near the cafeteria. It has a Marvel logo on it. Left it at security.",
        "Looking for a gym buddy! Anyone want to join me at the campus gym at 5 PM?",
        "The food at the new canteen stall is amazing! You guys have to try the Nasi Goreng there. 😋",
        "Is there any coding hackathon coming up this month? Looking for a team!",
        "Urgent! Does anyone have the lecture notes for Intro to Business Management week 4?",
        "Sunset view from the main building today was breathtaking. 📸",
        "Reminder: The scholarship application deadline is tomorrow! Don't forget to submit.",
        "Who's excited for the campus music festival next week? 🎸"
    )

    // --- GENERATE GROUPS ---
    fun seedGroups(onUpdate: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var batch = firestore.batch()
                var count = 0

                onUpdate("Generating groups...")

                universities.forEach { uniId ->
                    // Ambil 3 grup acak untuk setiap kampus
                    val selectedGroups = groupTemplates.shuffled().take(3)

                    selectedGroups.forEach { (name, desc) ->
                        val docRef = firestore.collection("groups").document()
                        val group = Group(
                            groupId = docRef.id,
                            name = "$name ($uniId)", // Contoh: Badminton Club (ITB)
                            description = desc,
                            universityId = uniId,
                            creatorId = "admin_bot", // Fake ID
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
                    // Ambil 5 postingan acak untuk setiap kampus
                    val selectedPosts = postTemplates.shuffled().take(5)

                    selectedPosts.forEach { text ->
                        val docRef = firestore.collection("posts").document()

                        // Fake Usernames
                        val fakeUser = listOf("alex_student", "sarah_j", "mike_99", "campus_life", "john_doe").random()

                        // Kita biarkan postId KOSONG di dalam objek,
                        // karena kita pakai @DocumentId di DataModels.
                        // Tapi kita set data ke docRef yang sudah punya ID.
                        val post = Post(
                            // postId = docRef.id, <--- JANGAN DIISI AGAR TIDAK CRASH
                            authorId = "dummy_user_${UUID.randomUUID()}",
                            authorName = fakeUser,
                            universityId = uniId,
                            text = text,
                            imageUrl = null,
                            timestamp = Date(),
                            voteCount = (0..50).random(),
                            commentCount = 0
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