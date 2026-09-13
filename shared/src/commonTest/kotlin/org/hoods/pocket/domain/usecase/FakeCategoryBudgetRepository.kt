package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class FakeCategoryBudgetRepository : CategoryBudgetRepository {
    private val budgetsFlow = MutableStateFlow<List<CategoryBudget>>(emptyList())

    var shouldFail: Boolean = false
    var failureMessage: String = "Database error"

    override fun getCategoryBudgets(yearMonth: YearMonth): Flow<List<CategoryBudget>> =
        budgetsFlow.map { list -> list.filter { it.yearMonth == yearMonth } }

    override suspend fun setCategoryBudget(budget: CategoryBudget) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        budgetsFlow.value = budgetsFlow.value.filterNot {
            it.yearMonth == budget.yearMonth && it.category == budget.category
        } + budget
    }

    override suspend fun setCategoryBudgets(budgets: List<CategoryBudget>) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        if (budgets.isEmpty()) return
        val targetMonth = budgets.first().yearMonth
        val targetCategories = budgets.map { it.category }.toSet()
        budgetsFlow.value = budgetsFlow.value.filterNot {
            it.yearMonth == targetMonth && it.category in targetCategories
        } + budgets
    }

    override suspend fun removeCategoryBudget(yearMonth: YearMonth, category: TransactionCategory) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        budgetsFlow.value = budgetsFlow.value.filterNot {
            it.yearMonth == yearMonth && it.category == category
        }
    }

    override suspend fun deleteCategoryBudgetsForMonth(yearMonth: YearMonth) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        budgetsFlow.value = budgetsFlow.value.filterNot {
            it.yearMonth == yearMonth
        }
    }
}
