package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class GetCategoryBudgetsUseCase(
    private val categoryBudgetRepository: CategoryBudgetRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<CategoryBudget>> {
        return categoryBudgetRepository.getCategoryBudgets(yearMonth)
    }
}
