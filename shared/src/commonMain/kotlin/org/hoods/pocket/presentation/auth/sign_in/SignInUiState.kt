package org.hoods.pocket.presentation.auth.sign_in

import org.hoods.pocket.domain.model.AuthUser

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSignUpMode: Boolean = false,
    val errorMessage: String? = null,
    val confirmationMessage: String? = null
)

sealed interface SignInEvent {
    data class SignInSuccess(val user: AuthUser) : SignInEvent
    data class ClaimRequired(val userId: String) : SignInEvent
    data object SignUpConfirmationSent : SignInEvent
}
