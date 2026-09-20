package org.hoods.pocket.data.repository

import org.hoods.pocket.data.local.dao.DataOwnershipDao
import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.repository.DataOwnershipRepository

class RoomDataOwnershipRepository(
    private val dao: DataOwnershipDao
) : DataOwnershipRepository {

    override suspend fun getUnclaimedDataSummary(): UnclaimedDataSummary {
        val txCount = dao.getUnclaimedTransactionCount()
        val budgetCount = dao.getUnclaimedMonthlyBudgetCount()
        val catCount = dao.getUnclaimedCategoryBudgetCount()
        return UnclaimedDataSummary(
            transactionCount = txCount,
            monthlyBudgetCount = budgetCount,
            categoryBudgetCount = catCount
        )
    }

    override suspend fun claimLocalDataAtomically(userId: String): Result<Unit> {
        return runCatching {
            dao.claimAllUnassigned(userId)
        }
    }
}
