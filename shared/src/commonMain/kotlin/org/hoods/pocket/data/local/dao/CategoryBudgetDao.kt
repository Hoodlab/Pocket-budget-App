package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.SyncOutboxEntity

@Dao
interface CategoryBudgetDao {

    @Query("SELECT * FROM category_budgets WHERE year = :year AND month = :month AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL)")
    fun getCategoryBudgets(year: Int, month: Int, activeUserId: String? = null): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE year = :year AND month = :month AND category = :category AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun getCategoryBudgetIncludingDeleted(year: Int, month: Int, category: String, activeUserId: String? = null): CategoryBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCategoryBudget(budget: CategoryBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCategoryBudgets(budgets: List<CategoryBudgetEntity>)

    @Query("DELETE FROM category_budgets WHERE year = :year AND month = :month AND category = :category AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteCategoryBudget(year: Int, month: Int, category: String, activeUserId: String? = null)

    @Query("UPDATE category_budgets SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE year = :year AND month = :month AND category = :category AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun softDeleteCategoryBudget(year: Int, month: Int, category: String, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long, activeUserId: String? = null)

    @Query("DELETE FROM category_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteCategoryBudgetsForMonth(year: Int, month: Int, activeUserId: String? = null)

    @Query("UPDATE category_budgets SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun softDeleteCategoryBudgetsForMonth(year: Int, month: Int, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long, activeUserId: String? = null)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOutbox(mutation: SyncOutboxEntity)

    @Transaction
    suspend fun insertOrUpdateWithOutbox(budget: CategoryBudgetEntity, outbox: SyncOutboxEntity?) {
        insertOrUpdateCategoryBudget(budget)
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }

    @Transaction
    suspend fun insertOrUpdateAllWithOutbox(budgets: List<CategoryBudgetEntity>, outboxes: List<SyncOutboxEntity>) {
        insertOrUpdateCategoryBudgets(budgets)
        for (outbox in outboxes) {
            insertOrUpdateOutbox(outbox)
        }
    }

    @Transaction
    suspend fun deleteWithOutbox(
        year: Int,
        month: Int,
        category: String,
        activeUserId: String?,
        isSoftDelete: Boolean,
        deletedAt: Long,
        updatedAt: Long,
        outbox: SyncOutboxEntity?
    ) {
        if (isSoftDelete) {
            softDeleteCategoryBudget(year, month, category, deletedAt, updatedAt, activeUserId)
        } else {
            deleteCategoryBudget(year, month, category, activeUserId)
        }
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }

    @Transaction
    suspend fun deleteForMonthWithOutbox(
        year: Int,
        month: Int,
        activeUserId: String?,
        isSoftDelete: Boolean,
        deletedAt: Long,
        updatedAt: Long,
        outboxes: List<SyncOutboxEntity>
    ) {
        if (isSoftDelete) {
            softDeleteCategoryBudgetsForMonth(year, month, deletedAt, updatedAt, activeUserId)
        } else {
            deleteCategoryBudgetsForMonth(year, month, activeUserId)
        }
        for (outbox in outboxes) {
            insertOrUpdateOutbox(outbox)
        }
    }
}
