package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity

@Dao
interface BudgetDao {

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    fun getMonthlyBudget(year: Int, month: Int, activeUserId: String? = null): Flow<MonthlyBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("DELETE FROM monthly_budgets WHERE year = :year AND month = :month AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteMonthlyBudget(year: Int, month: Int, activeUserId: String? = null)

    @Query("SELECT * FROM monthly_budgets WHERE (:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)")
    fun getAllBudgets(activeUserId: String? = null): Flow<List<MonthlyBudgetEntity>>
}
