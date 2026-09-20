package org.hoods.pocket.domain.model

data class AuthUser(
    val id: String,
    val email: String
)

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}
