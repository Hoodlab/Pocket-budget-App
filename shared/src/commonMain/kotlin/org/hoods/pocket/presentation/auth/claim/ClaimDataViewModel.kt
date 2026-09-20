package org.hoods.pocket.presentation.auth.claim

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
import org.hoods.pocket.domain.usecase.ClaimLocalDataUseCase
import org.hoods.pocket.domain.usecase.GetUnclaimedDataSummaryUseCase

class ClaimDataViewModel(
    private val targetUserId: String,
    private val getUnclaimedDataSummaryUseCase: GetUnclaimedDataSummaryUseCase,
    private val claimLocalDataUseCase: ClaimLocalDataUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ClaimDataUiState(targetUserId = targetUserId, isLoading = true))
    val uiState: StateFlow<ClaimDataUiState> = _uiState.asStateFlow()

    private val _events = Channel<ClaimDataEvent>(Channel.BUFFERED)
    val events: Flow<ClaimDataEvent> = _events.receiveAsFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val summary = getUnclaimedDataSummaryUseCase()
                _uiState.update { it.copy(unclaimedSummary = summary, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load summary") }
            }
        }
    }

    fun onClaimForAccount() {
        val current = _uiState.value
        if (current.isClaiming) return

        _uiState.update { it.copy(isClaiming = true, errorMessage = null) }
        scope.launch {
            val result = claimLocalDataUseCase(targetUserId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isClaiming = false) }
                _events.send(ClaimDataEvent.ClaimCompleted)
            } else {
                _uiState.update {
                    it.copy(
                        isClaiming = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to claim data"
                    )
                }
            }
        }
    }

    fun onKeepOnDevice() {
        scope.launch {
            _events.send(ClaimDataEvent.KeepOnDeviceCompleted)
        }
    }
}
