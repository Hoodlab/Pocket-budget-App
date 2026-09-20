package org.hoods.pocket.presentation.auth.claim

import org.hoods.pocket.domain.model.UnclaimedDataSummary

data class ClaimDataUiState(
    val targetUserId: String = "",
    val unclaimedSummary: UnclaimedDataSummary = UnclaimedDataSummary(0, 0, 0),
    val isLoading: Boolean = false,
    val isClaiming: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ClaimDataEvent {
    data object ClaimCompleted : ClaimDataEvent
    data object KeepOnDeviceCompleted : ClaimDataEvent
}
