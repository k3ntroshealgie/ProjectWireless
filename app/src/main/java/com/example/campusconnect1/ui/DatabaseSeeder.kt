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

    // 12 Kampus
    private val universities = listOf(
        "PU", "ITS", "UB", "IPB", "UNSRI",
        "TELKOMU", "UGM", "UNAIR", "UNDIP", "UNPAD", "UI", "ITB"
    )

    // Generates majors "001" to "010"
    private val majors = (1..10).map { String.format("%03d", it) }

    // 👇 TARGET TOTAL 1.000 PER KAMPUS
    // Karena ada 10 jurusan, maka per jurusan kita isi 100 orang.
    // 100 mhs x 10 jurusan = 1.000 mhs/kampus.
    // Total semua kampus = 12.000 dokumen (Aman untuk Free Tier)
    private val studentCountPerMajor = 100

    fun seedData(onUpdate: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var batch = firestore.batch()
                var operationCount = 0
                var totalUploaded = 0

                onUpdate("Memulai... Target total: 12.000 data.")

                universities.forEach { uniId ->
                    majors.forEach { majorCode ->

                        // Loop Mahasiswa (00001 s/d 00100)
                        for (i in 1..studentCountPerMajor) {
                            val sequence = String.format("%05d", i)
                            val generatedNim = "${majorCode}2024${sequence}"

                            val docRef = collection.document()
                            val data = AllowedNIM(
                                nim = generatedNim,
                                universityId = uniId,
                                role = "student"
                            )

                            batch.set(docRef, data)
                            operationCount++

                            // Kirim setiap 400 data
                            if (operationCount >= 400) {
                                batch.commit().await()
                                totalUploaded += operationCount

                                // Reset batch & counter
                                batch = firestore.batch()
                                operationCount = 0

                                // Kabari UI
                                onUpdate("Proses... Terupload: $totalUploaded / 12.000")
                            }
                        }
                    }
                }

                // Kirim sisa data di akhir
                if (operationCount > 0) {
                    batch.commit().await()
                    totalUploaded += operationCount
                }

                onUpdate("SELESAI! Total $totalUploaded NIM berhasil ditambahkan.")

            } catch (e: Exception) {
                Log.e("Seeder", "Gagal seed data", e)
                onUpdate("Error: ${e.message}")
            }
        }
    }
}