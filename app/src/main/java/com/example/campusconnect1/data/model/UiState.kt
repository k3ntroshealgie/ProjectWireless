package com.example.campusconnect1.data.model

/**
 * Sealed class representing UI state for data loading operations
 * Provides type-safe state management with loading, success, error, and idle states
 */
sealed class UiState<out T> {
    /**
     * Initial state before any operation
     */
    object Idle : UiState<Nothing>()
    
    /**
     * Loading state while operation is in progress
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Success state with loaded data
     * @param data The successfully loaded data
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state when operation fails
     * @param message User-friendly error message
     * @param throwable Optional exception for debugging
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
}

// Extension functions for easier state checking
fun <T> UiState<T>.isLoading() = this is UiState.Loading
fun <T> UiState<T>.isSuccess() = this is UiState.Success
fun <T> UiState<T>.isError() = this is UiState.Error
fun <T> UiState<T>.isIdle() = this is UiState.Idle

/**
 * Get data if state is Success, null otherwise
 */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

/**
 * Get error message if state is Error, null otherwise
 */
fun UiState<*>.errorMessageOrNull(): String? = (this as? UiState.Error)?.message
