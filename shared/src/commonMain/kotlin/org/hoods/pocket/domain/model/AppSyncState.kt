package org.hoods.pocket.domain.model

sealed interface AppSyncState {
    data object Idle : AppSyncState
    data class Pending(val pendingCount: Int) : AppSyncState
    data class Syncing(val pendingCount: Int) : AppSyncState
    data class Success(
        val lastSyncedAtEpochMillis: Long,
        val pushedCount: Int,
        val pulledCount: Int
    ) : AppSyncState
    data class Error(
        val message: String,
        val pendingCount: Int,
        val canRetry: Boolean
    ) : AppSyncState
}
