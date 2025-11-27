package com.example.campusconnect1.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// State Management for UI
enum class AuthState {
    IDLE, LOADING, SUCCESS, ERROR
}

data class AuthResult(
    val state: AuthState,
    val message: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authResult = MutableStateFlow(AuthResult(AuthState.IDLE))
    val authResult: StateFlow<AuthResult> = _authResult

    /**
     * Register user logic.
     * Automatically trims inputs to prevent format errors.
     */
    fun registerUser(
        email: String,
        pass: String,
        fullName: String,
        universityId: String,
        nim: String
    ) {
        viewModelScope.launch {
            _authResult.value = AuthResult(AuthState.LOADING)

            try {
                // 👇 PERBAIKAN UTAMA: Hapus spasi di depan/belakang (Trim)
                // Ini mengatasi error "badly formatted email" jika ada spasi tak terlihat
                val cleanEmail = email.trim()
                val cleanPass = pass.trim()
                val cleanNim = nim.trim()
                val cleanUniId = universityId.trim()
                val cleanName = fullName.trim()

                // 1. Create user in Firebase Authentication using CLEAN inputs
                val firebaseAuthResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                val firebaseUser = firebaseAuthResult.user
                    ?: throw IllegalStateException("Failed to create user")

                // 2. Check NIM in 'allowed_nims' collection
                val nimQuery = firestore.collection("allowed_nims")
                    .whereEqualTo("nim", cleanNim)
                    .whereEqualTo("universityId", cleanUniId)
                    .get()
                    .await()

                val isVerified = !nimQuery.isEmpty

                // 3. Create User Object
                val user = User(
                    uid = firebaseUser.uid,
                    email = cleanEmail, // Save the clean email
                    fullName = cleanName,
                    universityId = cleanUniId,
                    nim = cleanNim,

                    // Pastikan menggunakan 'verified' (huruf kecil) sesuai DataModels.kt
                    verified = isVerified,

                    interests = emptyList()
                )

                // 4. Save User Document to Firestore
                firestore.collection("users").document(firebaseUser.uid).set(user).await()

                val statusMessage = if (isVerified) "Verified Student" else "Unverified User"
                _authResult.value = AuthResult(AuthState.SUCCESS, "Registration Successful! ($statusMessage)")

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration Error", e)
                _authResult.value = AuthResult(AuthState.ERROR, e.message)
            }
        }
    }

    /**
     * Login logic.
     * Also trims email to ensure user can login even if they type a space.
     */
    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult(AuthState.LOADING)
            try {
                // 👇 PERBAIKAN: Trim email di sini juga
                val cleanEmail = email.trim()
                val cleanPass = pass.trim()

                auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                _authResult.value = AuthResult(AuthState.SUCCESS, "Login Successful!")
            } catch (e: Exception) {
                _authResult.value = AuthResult(AuthState.ERROR, e.message)
            }
        }
    }

    fun resetState() {
        _authResult.value = AuthResult(AuthState.IDLE)
    }
}