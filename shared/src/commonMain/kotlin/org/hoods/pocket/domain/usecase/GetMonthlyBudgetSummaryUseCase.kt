package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.MonthlyBudgetSummary
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.TransactionRepository

class GetMonthlyBudgetSummaryUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlyBudgetSummary> {
        val startMillis = yearMonth.startInstant().toEpochMilliseconds()
        val endMillis = yearMonth.endInstant().toEpochMilliseconds()

        val budgetFlow = budgetRepository.getMonthlyBudget(yearMonth)
        val expenseFlow = transactionRepository.getExpenseTotalByDateRange(startMillis, endMillis)

        return combine(budgetFlow, expenseFlow) { budget, totalExpense ->
            val remainingAmount = budget?.let { it.limitAmount - totalExpense }
            val progress = if (budget != null && budget.limitAmount > 0L) {
                (totalExpense.toFloat() / budget.limitAmount.toFloat()).coerceAtLeast(0f)
            } else {
                0f
            }
            val status = if (budget != null) {
                when {
                    totalExpense > budget.limitAmount -> BudgetHealthStatus.OVER_BUDGET
                    totalExpense * 10L >= budget.limitAmount * 8L -> BudgetHealthStatus.NEAR_LIMIT
                    else -> BudgetHealthStatus.ON_TRACK
                }
            } else {
                null
            }

            MonthlyBudgetSummary(
                yearMonth = yearMonth,
                budget = budget,
                totalExpense = totalExpense,
                remainingAmount = remainingAmount,
                progress = progress,
                status = status
            )
        }
    }
}
