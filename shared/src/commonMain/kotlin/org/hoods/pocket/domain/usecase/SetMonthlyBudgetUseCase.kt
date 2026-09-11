package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

class SetMonthlyBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(yearMonth: YearMonth, amountCents: Long): Result<MonthlyBudget> {
        if (amountCents <= 0L) {
            return Result.failure(IllegalArgumentException("Budget limit must be greater than zero"))
        }
        val budget = MonthlyBudget(yearMonth = yearMonth, limitAmount = amountCents)
        budgetRepository.setMonthlyBudget(budget)
        return Result.success(budget)
    }
}
