package com.example.campusconnect1.ml

import java.util.Locale
import kotlin.math.ln
import kotlin.math.log

/**
 * A simple Naive Bayes Classifier for text moderation.
 * This runs entirely on-device and does not require external models.
 */
object TextClassifier {

    // --- 1. Training Data (Embedded) ---
    // In a real app, this could be loaded from a JSON file or updated remotely.
    private val toxicData = listOf(
        "bodoh", "goblok", "tolol", "anjing", "babi", "bangsat", "kampret",
        "mati aja", "bunuh diri", "sampah", "jelek banget", "idiot", "moron",
        "stupid", "fuck", "shit", "bitch", "asshole", "kill yourself", "hate you",
        "dasar bego", "otak udang", "tidak berguna", "menjijikkan",
        "saya akan bunuh diri", "ingin mati", "i want to die", "go to hell",
        "dasar lemah", "pecundang", "loser", "freak"
    )

    private val safeData = listOf(
        "hebat", "keren", "bagus", "terima kasih", "semangat", "selamat",
        "good job", "nice", "awesome", "thank you", "congrats", "love it",
        "belajar", "kuliah", "kampus", "dosen", "mahasiswa", "tugas",
        "buku", "perpustakaan", "diskusi", "kelompok", "teman", "sahabat",
        "hari yang indah", "semoga sukses", "jangan menyerah", "ayo berjuang",
        "test", "testing", "hello", "check", "coba", "percobaan", "halo", "hai"
    )

    // --- 2. Model State ---
    private var logPriorToxic: Double = 0.0
    private var logPriorSafe: Double = 0.0
    private val logLikelihoodToxic = mutableMapOf<String, Double>()
    private val logLikelihoodSafe = mutableMapOf<String, Double>()
    private val vocab = mutableSetOf<String>()

    init {
        train()
    }

    // --- 3. Training Logic ---
    private fun train() {
        val toxicDocs = toxicData.map { tokenize(it) }
        val safeDocs = safeData.map { tokenize(it) }

        // Calculate Priors
        val totalDocs = toxicDocs.size + safeDocs.size
        logPriorToxic = ln(toxicDocs.size.toDouble() / totalDocs)
        logPriorSafe = ln(safeDocs.size.toDouble() / totalDocs)

        // Build Vocabulary
        toxicDocs.forEach { vocab.addAll(it) }
        safeDocs.forEach { vocab.addAll(it) }

        // Calculate Likelihoods
        val toxicWordCounts = countWords(toxicDocs)
        val safeWordCounts = countWords(safeDocs)

        val totalToxicWords = toxicWordCounts.values.sum()
        val totalSafeWords = safeWordCounts.values.sum()
        val vocabSize = vocab.size

        // Laplace Smoothing (Add-1 Smoothing)
        for (word in vocab) {
            val countInToxic = toxicWordCounts[word] ?: 0
            val countInSafe = safeWordCounts[word] ?: 0

            logLikelihoodToxic[word] = ln((countInToxic + 1).toDouble() / (totalToxicWords + vocabSize))
            logLikelihoodSafe[word] = ln((countInSafe + 1).toDouble() / (totalSafeWords + vocabSize))
        }
    }

    // --- 4. Classification Logic ---
    fun classify(text: String): ClassificationResult {
        val tokens = tokenize(text)
        var logProbToxic = logPriorToxic
        var logProbSafe = logPriorSafe

        for (token in tokens) {
            if (vocab.contains(token)) {
                logProbToxic += logLikelihoodToxic[token]!!
                logProbSafe += logLikelihoodSafe[token]!!
            }
        }

        // Convert log probabilities back to probability space (roughly)
        // We just need to know which one is greater, but for "confidence" we can do a softmax-like approach
        // For simplicity, we just check if Toxic > Safe and by how much margin.
        
        val isToxic = logProbToxic > logProbSafe
        
        // Simple confidence score (not a true probability, but useful for thresholding)
        // If the difference is small, we might be unsure.
        val margin = logProbToxic - logProbSafe
        
        // Threshold: If margin is very small, maybe let it pass or flag as suspicious.
        // Here we'll be strict: if it's more likely toxic, it's toxic.
        
        return ClassificationResult(
            isToxic = isToxic,
            confidence = margin // Positive = Toxic, Negative = Safe
        )
    }

    // --- 5. Helper Functions ---
    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove punctuation
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    private fun countWords(docs: List<List<String>>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (doc in docs) {
            for (word in doc) {
                counts[word] = (counts[word] ?: 0) + 1
            }
        }
        return counts
    }
}

data class ClassificationResult(
    val isToxic: Boolean,
    val confidence: Double
)
