package org.hoods.pocket.presentation.auth.account

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
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.remote.SupabaseEnvironment
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.usecase.GetAuthStateUseCase
import org.hoods.pocket.domain.usecase.SignOutUseCase

class AccountViewModel(
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val config: SupabaseConfig,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        AccountUiState(
            environmentName = when (config.environment) {
                SupabaseEnvironment.LOCAL -> "Local Supabase (127.0.0.1:54321)"
                SupabaseEnvironment.HOSTED_DEV -> "Hosted Dev (hyetpnyruynichpsxyvz)"
                SupabaseEnvironment.UNCONFIGURED -> "Offline (Unconfigured)"
            }
        )
    )
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = Channel<AccountEvent>(Channel.BUFFERED)
    val events: Flow<AccountEvent> = _events.receiveAsFlow()

    init {
        scope.launch {
            getAuthStateUseCase().collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                userEmail = authState.user.email,
                                isAuthenticated = true
                            )
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.update {
                            it.copy(
                                userEmail = null,
                                isAuthenticated = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun onSignOut() {
        val current = _uiState.value
        if (current.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            val result = signOutUseCase()
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _events.send(AccountEvent.SignOutSuccess)
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to sign out")
                }
            }
        }
    }

    fun onNavigateToAuth() {
        scope.launch {
            _events.send(AccountEvent.NavigateToAuth)
        }
    }

    fun onNavigateToSetup() {
        scope.launch {
            _events.send(AccountEvent.NavigateToSetup)
        }
    }
}
