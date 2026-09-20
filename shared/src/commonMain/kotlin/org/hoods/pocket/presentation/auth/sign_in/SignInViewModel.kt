package org.hoods.pocket.presentation.auth.sign_in

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hoods.pocket.domain.repository.AuthResult
import org.hoods.pocket.domain.usecase.GetUnclaimedDataSummaryUseCase
import org.hoods.pocket.domain.usecase.SignInUseCase
import org.hoods.pocket.domain.usecase.SignUpUseCase

class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val getUnclaimedDataSummaryUseCase: GetUnclaimedDataSummaryUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = _events.receiveAsFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null, confirmationMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null, confirmationMessage = null) }
    }

    fun onToggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                confirmationMessage = null
            )
        }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading) return

        val email = current.email.trim()
        val password = current.password

        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, confirmationMessage = null) }

        scope.launch {
            val result = if (current.isSignUpMode) {
                signUpUseCase(email, password)
            } else {
                signInUseCase(email, password)
            }

            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    val summary = getUnclaimedDataSummaryUseCase()
                    if (summary.hasUnclaimedData) {
                        _events.send(SignInEvent.ClaimRequired(result.user.id))
                    } else {
                        _events.send(SignInEvent.SignInSuccess(result.user))
                    }
                }
                is AuthResult.ConfirmationRequired -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            confirmationMessage = "Confirmation email sent to ${result.email}. Please verify your email before signing in."
                        )
                    }
                    _events.send(SignInEvent.SignUpConfirmationSent)
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }
}
