package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.SyncOutboxEntity

@Dao
interface BudgetDao {

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL)")
    fun getMonthlyBudget(year: Int, month: Int, activeUserId: String? = null): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun getMonthlyBudgetIncludingDeleted(year: Int, month: Int, activeUserId: String? = null): MonthlyBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("DELETE FROM monthly_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteMonthlyBudget(year: Int, month: Int, activeUserId: String? = null)

    @Query("UPDATE monthly_budgets SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun softDeleteMonthlyBudget(year: Int, month: Int, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long, activeUserId: String? = null)

    @Query("SELECT * FROM monthly_budgets WHERE (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL)")
    fun getAllBudgets(activeUserId: String? = null): Flow<List<MonthlyBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOutbox(mutation: SyncOutboxEntity)

    @Transaction
    suspend fun insertOrUpdateWithOutbox(budget: MonthlyBudgetEntity, outbox: SyncOutboxEntity?) {
        insertOrUpdateMonthlyBudget(budget)
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }

    @Transaction
    suspend fun deleteWithOutbox(
        year: Int,
        month: Int,
        activeUserId: String?,
        isSoftDelete: Boolean,
        deletedAt: Long,
        updatedAt: Long,
        outbox: SyncOutboxEntity?
    ) {
        if (isSoftDelete) {
            softDeleteMonthlyBudget(year, month, deletedAt, updatedAt, activeUserId)
        } else {
            deleteMonthlyBudget(year, month, activeUserId)
        }
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }
}
