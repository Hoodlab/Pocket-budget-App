package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class RemoveCategoryBudgetUseCase(
    private val categoryBudgetRepository: CategoryBudgetRepository
) {
    suspend operator fun invoke(
        yearMonth: YearMonth,
        category: TransactionCategory
    ): Result<Unit> = runCatching {
        categoryBudgetRepository.removeCategoryBudget(yearMonth, category)
    }
}
