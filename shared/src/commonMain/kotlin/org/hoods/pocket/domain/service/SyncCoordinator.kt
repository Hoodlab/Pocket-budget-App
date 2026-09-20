package org.hoods.pocket.domain.service

import kotlinx.coroutines.flow.StateFlow
import org.hoods.pocket.domain.model.AppSyncState

interface SyncCoordinator {
    val syncState: StateFlow<AppSyncState>
    fun requestSync(): kotlinx.coroutines.Job
}
