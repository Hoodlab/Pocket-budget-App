package org.hoods.pocket.data.repository

import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.repository.DataOwnershipRepository

class FakeDataOwnershipRepository(
    var summary: UnclaimedDataSummary = UnclaimedDataSummary(3, 1, 2),
    var shouldFailClaim: Boolean = false
) : DataOwnershipRepository {
    var claimedUserId: String? = null

    override suspend fun getUnclaimedDataSummary(): UnclaimedDataSummary = summary

    override suspend fun claimLocalDataAtomically(userId: String): Result<Unit> {
        return if (shouldFailClaim) {
            Result.failure(Exception("Claiming failed"))
        } else {
            claimedUserId = userId
            summary = UnclaimedDataSummary(0, 0, 0)
            Result.success(Unit)
        }
    }
}
