package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.repository.DataOwnershipRepository

class ClaimLocalDataUseCase(
    private val repository: DataOwnershipRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return repository.claimLocalDataAtomically(userId)
    }
}
