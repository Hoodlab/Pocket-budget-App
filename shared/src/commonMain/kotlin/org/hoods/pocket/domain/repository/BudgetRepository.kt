package org.hoods.pocket.domain.repository

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth

interface BudgetRepository {
    fun getMonthlyBudget(yearMonth: YearMonth): Flow<MonthlyBudget?>
    suspend fun setMonthlyBudget(budget: MonthlyBudget)
    suspend fun deleteMonthlyBudget(yearMonth: YearMonth)
}
