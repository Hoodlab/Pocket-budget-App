package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

class DeleteMonthlyBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(yearMonth: YearMonth): Result<Unit> {
        return runCatching {
            budgetRepository.deleteMonthlyBudget(yearMonth)
        }
    }
}
