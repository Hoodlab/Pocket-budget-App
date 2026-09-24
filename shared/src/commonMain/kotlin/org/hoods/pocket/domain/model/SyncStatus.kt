package org.hoods.pocket.domain.model

sealed interface SyncStatus {
    data object SignedOut : SyncStatus
    data object Unconfigured : SyncStatus
    data class Offline(val pendingCount: Int) : SyncStatus
    data class Pending(val pendingCount: Int) : SyncStatus
    data class Syncing(val pendingCount: Int) : SyncStatus
    data class Success(
        val lastSyncedAtEpochMillis: Long,
        val pushedCount: Int,
        val pulledCount: Int
    ) : SyncStatus
    data class Failure(
        val message: String,
        val pendingCount: Int,
        val canRetry: Boolean = true
    ) : SyncStatus
}
