package org.hoods.pocket.domain.repository

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth

interface CategoryBudgetRepository {
    fun getCategoryBudgets(yearMonth: YearMonth): Flow<List<CategoryBudget>>
    suspend fun setCategoryBudget(budget: CategoryBudget)
    suspend fun setCategoryBudgets(budgets: List<CategoryBudget>)
    suspend fun removeCategoryBudget(yearMonth: YearMonth, category: TransactionCategory)
    suspend fun deleteCategoryBudgetsForMonth(yearMonth: YearMonth)
}
