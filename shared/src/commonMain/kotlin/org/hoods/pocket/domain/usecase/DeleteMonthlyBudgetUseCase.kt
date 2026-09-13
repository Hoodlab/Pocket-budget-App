package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class DeleteMonthlyBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository? = null
) {
    suspend operator fun invoke(yearMonth: YearMonth): Result<Unit> {
        return runCatching {
            categoryBudgetRepository?.deleteCategoryBudgetsForMonth(yearMonth)
            budgetRepository.deleteMonthlyBudget(yearMonth)
        }
    }
}
