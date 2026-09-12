package org.hoods.pocket.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

class InMemoryBudgetRepository : BudgetRepository {
    private val budgets = MutableStateFlow<Map<YearMonth, MonthlyBudget>>(emptyMap())

    override fun getMonthlyBudget(yearMonth: YearMonth): Flow<MonthlyBudget?> {
        return budgets.map { it[yearMonth] }
    }

    override suspend fun setMonthlyBudget(budget: MonthlyBudget) {
        budgets.value = budgets.value + (budget.yearMonth to budget)
    }

    override suspend fun deleteMonthlyBudget(yearMonth: YearMonth) {
        budgets.value = budgets.value - yearMonth
    }
}
