package org.hoods.pocket.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class InMemoryCategoryBudgetRepository : CategoryBudgetRepository {
    private val storedBudgets = MutableStateFlow<List<CategoryBudget>>(emptyList())

    override fun getCategoryBudgets(yearMonth: YearMonth): Flow<List<CategoryBudget>> {
        return storedBudgets.map { list ->
            list.filter { it.yearMonth == yearMonth }
        }
    }

    override suspend fun setCategoryBudget(budget: CategoryBudget) {
        storedBudgets.value = storedBudgets.value.filterNot {
            it.yearMonth == budget.yearMonth && it.category == budget.category
        } + budget
    }

    override suspend fun setCategoryBudgets(budgets: List<CategoryBudget>) {
        if (budgets.isEmpty()) return
        val targetMonth = budgets.first().yearMonth
        val targetCategories = budgets.map { it.category }.toSet()
        storedBudgets.value = storedBudgets.value.filterNot {
            it.yearMonth == targetMonth && it.category in targetCategories
        } + budgets
    }

    override suspend fun removeCategoryBudget(yearMonth: YearMonth, category: TransactionCategory) {
        storedBudgets.value = storedBudgets.value.filterNot {
            it.yearMonth == yearMonth && it.category == category
        }
    }

    override suspend fun deleteCategoryBudgetsForMonth(yearMonth: YearMonth) {
        storedBudgets.value = storedBudgets.value.filterNot {
            it.yearMonth == yearMonth
        }
    }
}
