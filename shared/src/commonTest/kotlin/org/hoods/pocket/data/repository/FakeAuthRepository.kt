package org.hoods.pocket.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.AuthResult

class FakeAuthRepository(
    initialState: AuthState = AuthState.Unauthenticated,
    var shouldRequireConfirmation: Boolean = false,
    var errorToReturn: String? = null
) : AuthRepository {

    private val _authState = MutableStateFlow(initialState)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signUp(email: String, password: String): AuthResult {
        if (errorToReturn != null) return AuthResult.Error(errorToReturn!!)
        if (shouldRequireConfirmation) {
            return AuthResult.ConfirmationRequired(email)
        }
        val user = AuthUser(id = "user-${email.hashCode()}", email = email)
        _authState.value = AuthState.Authenticated(user)
        return AuthResult.Success(user)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        if (errorToReturn != null) return AuthResult.Error(errorToReturn!!)
        val user = AuthUser(id = "user-${email.hashCode()}", email = email)
        _authState.value = AuthState.Authenticated(user)
        return AuthResult.Success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override fun currentUser(): AuthUser? {
        val state = _authState.value
        return if (state is AuthState.Authenticated) state.user else null
    }

    fun setAuthenticated(user: AuthUser) {
        _authState.value = AuthState.Authenticated(user)
    }
}
