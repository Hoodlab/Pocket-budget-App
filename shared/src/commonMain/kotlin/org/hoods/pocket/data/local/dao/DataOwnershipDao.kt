package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.SyncOutboxEntity
import org.hoods.pocket.data.local.entity.TransactionEntity

@Dao
interface DataOwnershipDao {

    @Query("SELECT COUNT(*) FROM transactions WHERE userId IS NULL AND deletedAtEpochMillis IS NULL")
    suspend fun getUnclaimedTransactionCount(): Int

    @Query("SELECT COUNT(*) FROM monthly_budgets WHERE userId IS NULL AND deletedAtEpochMillis IS NULL")
    suspend fun getUnclaimedMonthlyBudgetCount(): Int

    @Query("SELECT COUNT(*) FROM category_budgets WHERE userId IS NULL AND deletedAtEpochMillis IS NULL")
    suspend fun getUnclaimedCategoryBudgetCount(): Int

    @Query("SELECT * FROM transactions WHERE userId IS NULL AND deletedAtEpochMillis IS NULL")
    suspend fun getUnclaimedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM monthly_budgets WHERE userId IS NULL AND deletedAtEpochMillis IS NULL")
    suspend fun getUnclaimedMonthlyBudgets(): List<MonthlyBudgetEntity>

    @Query("SELECT * FROM category_budgets WHERE userId IS NULL AND deletedAtEpochMillis IS NULL")
    suspend fun getUnclaimedCategoryBudgets(): List<CategoryBudgetEntity>

    @Query("UPDATE transactions SET userId = :userId, updatedAtEpochMillis = :updatedAtEpochMillis WHERE userId IS NULL")
    suspend fun claimTransactions(userId: String, updatedAtEpochMillis: Long): Int

    @Query("UPDATE monthly_budgets SET userId = :userId, updatedAtEpochMillis = :updatedAtEpochMillis WHERE userId IS NULL")
    suspend fun claimMonthlyBudgets(userId: String, updatedAtEpochMillis: Long): Int

    @Query("UPDATE category_budgets SET userId = :userId, updatedAtEpochMillis = :updatedAtEpochMillis WHERE userId IS NULL")
    suspend fun claimCategoryBudgets(userId: String, updatedAtEpochMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxMutations(mutations: List<SyncOutboxEntity>)

    @Transaction
    suspend fun claimAllUnassigned(userId: String) {
        val now = 0L
        claimTransactions(userId, now)
        claimMonthlyBudgets(userId, now)
        claimCategoryBudgets(userId, now)
    }

    @Transaction
    suspend fun claimAllUnassignedWithMutations(
        userId: String,
        updatedAtEpochMillis: Long,
        mutations: List<SyncOutboxEntity>
    ) {
        claimTransactions(userId, updatedAtEpochMillis)
        claimMonthlyBudgets(userId, updatedAtEpochMillis)
        claimCategoryBudgets(userId, updatedAtEpochMillis)
        if (mutations.isNotEmpty()) {
            insertOutboxMutations(mutations)
        }
    }
}
