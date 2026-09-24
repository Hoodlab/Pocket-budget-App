package org.hoods.pocket.presentation.auth.account

import org.hoods.pocket.domain.model.SyncStatus

data class AccountUiState(
    val userEmail: String? = null,
    val isAuthenticated: Boolean = false,
    val environmentName: String = "Local Offline",
    val syncStatus: SyncStatus = SyncStatus.SignedOut,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canSyncNow: Boolean
        get() = isAuthenticated &&
            syncStatus !is SyncStatus.Unconfigured &&
            syncStatus !is SyncStatus.Offline &&
            syncStatus !is SyncStatus.Syncing

    val canRetry: Boolean
        get() = syncStatus is SyncStatus.Failure && syncStatus.canRetry
}

sealed interface AccountEvent {
    data object SignOutSuccess : AccountEvent
    data object NavigateToAuth : AccountEvent
    data object NavigateToSetup : AccountEvent
}
