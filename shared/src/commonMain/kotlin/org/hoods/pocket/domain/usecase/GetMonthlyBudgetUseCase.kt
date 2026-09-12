package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

class GetMonthlyBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlyBudget?> {
        return budgetRepository.getMonthlyBudget(yearMonth)
    }
}
