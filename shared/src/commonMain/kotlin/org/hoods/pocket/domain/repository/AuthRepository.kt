package org.hoods.pocket.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class ConfirmationRequired(val email: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signOut(): Result<Unit>
    fun currentUser(): AuthUser?
}
