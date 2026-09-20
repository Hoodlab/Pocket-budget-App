package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.repository.DataOwnershipRepository

class GetUnclaimedDataSummaryUseCase(
    private val repository: DataOwnershipRepository
) {
    suspend operator fun invoke(): UnclaimedDataSummary {
        return repository.getUnclaimedDataSummary()
    }
}
