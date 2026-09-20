package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity

@Dao
interface CategoryBudgetDao {

    @Query("SELECT * FROM category_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    fun getCategoryBudgets(year: Int, month: Int, activeUserId: String? = null): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCategoryBudget(budget: CategoryBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCategoryBudgets(budgets: List<CategoryBudgetEntity>)

    @Query("DELETE FROM category_budgets WHERE year = :year AND month = :month AND category = :category AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteCategoryBudget(year: Int, month: Int, category: String, activeUserId: String? = null)

    @Query("DELETE FROM category_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteCategoryBudgetsForMonth(year: Int, month: Int, activeUserId: String? = null)
}
