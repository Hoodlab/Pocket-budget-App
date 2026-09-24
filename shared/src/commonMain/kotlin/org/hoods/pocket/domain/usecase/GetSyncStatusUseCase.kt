package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.SyncStatus
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.hoods.pocket.domain.service.SyncCoordinator

class GetSyncStatusUseCase(
    private val authRepository: AuthRepository,
    private val syncCoordinator: SyncCoordinator,
    private val connectivityObserver: ConnectivityObserver,
    private val isConfigured: () -> Boolean
) {
    operator fun invoke(): Flow<SyncStatus> {
        return combine(
            authRepository.authState,
            connectivityObserver.isOnline,
            syncCoordinator.syncState,
            syncCoordinator.pendingCount
        ) { authState, isOnline, syncState, pendingCount ->
            when {
                authState is AuthState.Unauthenticated -> SyncStatus.SignedOut
                !isConfigured() -> SyncStatus.Unconfigured
                !isOnline -> SyncStatus.Offline(pendingCount = pendingCount)
                syncState is AppSyncState.Syncing -> SyncStatus.Syncing(pendingCount = syncState.pendingCount)
                syncState is AppSyncState.Error -> SyncStatus.Failure(
                    message = syncState.message,
                    pendingCount = syncState.pendingCount,
                    canRetry = syncState.canRetry
                )
                syncState is AppSyncState.Pending -> SyncStatus.Pending(pendingCount = syncState.pendingCount)
                syncState is AppSyncState.Success -> {
                    if (pendingCount > 0) {
                        SyncStatus.Pending(pendingCount = pendingCount)
                    } else {
                        SyncStatus.Success(
                            lastSyncedAtEpochMillis = syncState.lastSyncedAtEpochMillis,
                            pushedCount = syncState.pushedCount,
                            pulledCount = syncState.pulledCount
                        )
                    }
                }
                syncState is AppSyncState.Idle -> {
                    if (pendingCount > 0) {
                        SyncStatus.Pending(pendingCount = pendingCount)
                    } else {
                        SyncStatus.Success(
                            lastSyncedAtEpochMillis = 0L,
                            pushedCount = 0,
                            pulledCount = 0
                        )
                    }
                }
                else -> SyncStatus.SignedOut
            }
        }
    }
}
