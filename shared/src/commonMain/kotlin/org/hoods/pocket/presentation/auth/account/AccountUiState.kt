package org.hoods.pocket.presentation.auth.account

data class AccountUiState(
    val userEmail: String? = null,
    val isAuthenticated: Boolean = false,
    val environmentName: String = "Local Offline",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AccountEvent {
    data object SignOutSuccess : AccountEvent
    data object NavigateToAuth : AccountEvent
    data object NavigateToSetup : AccountEvent
}
