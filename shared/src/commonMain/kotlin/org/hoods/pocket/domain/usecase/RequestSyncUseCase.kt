package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.Job
import org.hoods.pocket.domain.service.SyncCoordinator

class RequestSyncUseCase(
    private val syncCoordinator: SyncCoordinator
) {
    operator fun invoke(): Job {
        return syncCoordinator.requestSync()
    }
}
