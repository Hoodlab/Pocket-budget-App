package org.hoods.pocket.domain.repository

import org.hoods.pocket.domain.model.UnclaimedDataSummary

interface DataOwnershipRepository {
    suspend fun getUnclaimedDataSummary(): UnclaimedDataSummary
    suspend fun claimLocalDataAtomically(userId: String): Result<Unit>
}
