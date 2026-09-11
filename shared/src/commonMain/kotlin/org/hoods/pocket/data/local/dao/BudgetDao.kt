package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity

@Dao
interface BudgetDao {

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month")
    fun getMonthlyBudget(year: Int, month: Int): Flow<MonthlyBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budgets")
    fun getAllBudgets(): Flow<List<MonthlyBudgetEntity>>
}
