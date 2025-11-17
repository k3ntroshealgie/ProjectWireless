package com.example.campusconnect1.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect1.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// State Management
enum class AuthState {
    IDLE, LOADING, SUCCESS, ERROR
}

data class AuthResult(
    val state: AuthState,
    val message: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authResult = MutableStateFlow(AuthResult(AuthState.IDLE))
    val authResult: StateFlow<AuthResult> = _authResult

    /**
     * Register user logic.
     * 1. Create Auth User
     * 2. Check NIM validity in Firestore
     * 3. Save User Profile to Firestore
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
                // 1. Create user in Firebase Authentication
                val firebaseAuthResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = firebaseAuthResult.user
                    ?: throw IllegalStateException("Failed to create user")

                // 2. Clean Inputs (Remove extra spaces)
                // This is crucial for matching database exactly
                val cleanNim = nim.trim()
                val cleanUniId = universityId.trim()

                // 3. Check NIM in 'allowed_nims' collection
                // We look for a document where BOTH 'nim' and 'universityId' match
                val nimQuery = firestore.collection("allowed_nims")
                    .whereEqualTo("nim", cleanNim)
                    .whereEqualTo("universityId", cleanUniId)
                    .get()
                    .await()

                // If the query returns any documents, it means the NIM is valid
                val isVerified = !nimQuery.isEmpty

                // 4. Create User Object
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    fullName = fullName,
                    universityId = cleanUniId, // Use the cleaned ID
                    nim = cleanNim,            // Use the cleaned NIM
                    verified = isVerified,
                    interests = emptyList()
                )

                // 5. Save User Document to Firestore
                firestore.collection("users").document(firebaseUser.uid).set(user).await()

                // 6. Success!
                val statusMessage = if (isVerified) "Verified Student" else "Unverified User"
                _authResult.value = AuthResult(AuthState.SUCCESS, "Registration Successful! ($statusMessage)")

            } catch (e: Exception) {
                // Log the error for debugging
                Log.e("AuthViewModel", "Registration Error", e)
                _authResult.value = AuthResult(AuthState.ERROR, e.message)
            }
        }
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult(AuthState.LOADING)
            try {
                // 1. Proses Login ke Firebase Auth
                auth.signInWithEmailAndPassword(email, pass).await()

                // 2. Jika berhasil, AuthState jadi SUCCESS
                // Kita tidak perlu cek isVerified di sini, karena pengecekan
                // dilakukan saat mau posting (CreatePostViewModel).
                _authResult.value = AuthResult(AuthState.SUCCESS, "Login Successful!")

            } catch (e: Exception) {
                _authResult.value = AuthResult(AuthState.ERROR, e.message)
            }
        }
    }

    /**
     * Reset state to IDLE (e.g. after showing an error dialog)
     */
    fun resetState() {
        _authResult.value = AuthResult(AuthState.IDLE)
    }
}