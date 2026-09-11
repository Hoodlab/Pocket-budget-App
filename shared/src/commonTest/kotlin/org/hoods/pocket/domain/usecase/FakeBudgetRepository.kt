package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

class FakeBudgetRepository : BudgetRepository {
    private val budgetsFlow = MutableStateFlow<Map<YearMonth, MonthlyBudget>>(emptyMap())

    var shouldFail: Boolean = false
    var failureMessage: String = "Database error"

    override fun getMonthlyBudget(yearMonth: YearMonth): Flow<MonthlyBudget?> =
        budgetsFlow.map { it[yearMonth] }

    override suspend fun setMonthlyBudget(budget: MonthlyBudget) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        budgetsFlow.value = budgetsFlow.value + (budget.yearMonth to budget)
    }
}
