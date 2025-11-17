package com.example.campusconnect1.ui

import android.util.Log
import com.example.campusconnect1.AllowedNIM
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DatabaseSeeder {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("allowed_nims")

    // Daftar Kampus sesuai permintaan
    private val universities = listOf(
        "PU", "ITS", "UB", "IPB", "UNSRI",
        "TELKOMU", "UGM", "UNAIR", "UNDIP", "UNPAD", "UI", "ITB"
    )

    // Kode Jurusan: 001 (IT), 002 (Bisnis), 003 (Manajemen)
    private val majors = listOf("001", "002", "003")

    fun seedData(onComplete: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val batch = firestore.batch() // Batch agar prosesnya cepat (sekaligus)
                var count = 0

                universities.forEach { uniId ->
                    majors.forEach { majorCode ->
                        // Kita buat 5 Mahasiswa per jurusan di setiap kampus
                        // Pattern: KODE_JURUSAN + TAHUN(2024) + URUTAN(0000X)
                        // Contoh: 001202400001
                        for (i in 1..5) {
                            val sequence = String.format("%05d", i) // Mengubah 1 jadi "00001"
                            val generatedNim = "${majorCode}2024${sequence}"

                            // Data Mahasiswa
                            val docRefStudent = collection.document() // Auto ID
                            val dataStudent = AllowedNIM(
                                nim = generatedNim,
                                universityId = uniId,
                                role = "student"
                            )
                            batch.set(docRefStudent, dataStudent)
                            count++
                        }

                        // Kita tambahkan 1 Dosen per jurusan
                        // Format NIM Dosen beda dikit, misal depannya 999
                        val dosenNim = "999${majorCode}202401"
                        val docRefDosen = collection.document()
                        val dataDosen = AllowedNIM(
                            nim = dosenNim,
                            universityId = uniId,
                            role = "dosen"
                        )
                        batch.set(docRefDosen, dataDosen)
                        count++
                    }
                }

                batch.commit().await()
                onComplete("Sukses! Berhasil menambahkan $count data NIM ke Database.")

            } catch (e: Exception) {
                Log.e("Seeder", "Gagal seed data", e)
                onComplete("Error: ${e.message}")
            }
        }
    }
}