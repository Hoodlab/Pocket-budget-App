package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DataOwnershipDao {

    @Query("SELECT COUNT(*) FROM transactions WHERE userId IS NULL")
    suspend fun getUnclaimedTransactionCount(): Int

    @Query("SELECT COUNT(*) FROM monthly_budgets WHERE userId IS NULL")
    suspend fun getUnclaimedMonthlyBudgetCount(): Int

    @Query("SELECT COUNT(*) FROM category_budgets WHERE userId IS NULL")
    suspend fun getUnclaimedCategoryBudgetCount(): Int

    @Query("UPDATE transactions SET userId = :userId WHERE userId IS NULL")
    suspend fun claimTransactions(userId: String): Int

    @Query("UPDATE monthly_budgets SET userId = :userId WHERE userId IS NULL")
    suspend fun claimMonthlyBudgets(userId: String): Int

    @Query("UPDATE category_budgets SET userId = :userId WHERE userId IS NULL")
    suspend fun claimCategoryBudgets(userId: String): Int

    @Transaction
    suspend fun claimAllUnassigned(userId: String) {
        claimTransactions(userId)
        claimMonthlyBudgets(userId)
        claimCategoryBudgets(userId)
    }
}
