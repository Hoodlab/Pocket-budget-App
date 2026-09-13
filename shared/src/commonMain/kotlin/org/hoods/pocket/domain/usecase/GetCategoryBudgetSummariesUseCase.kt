package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.CategoryBudgetSummary
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.TransactionRepository

class GetCategoryBudgetSummariesUseCase(
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<CategoryBudgetSummary>> {
        val startMillis = yearMonth.startInstant().toEpochMilliseconds()
        val endMillis = yearMonth.endInstant().toEpochMilliseconds()

        val budgetsFlow = categoryBudgetRepository.getCategoryBudgets(yearMonth)
        val spendingFlow = transactionRepository.getCategorySpendingByDateRange(startMillis, endMillis)

        return combine(budgetsFlow, spendingFlow) { budgets, spendingMap ->
            budgets.map { budget ->
                val spent = spendingMap[budget.category] ?: 0L
                val remaining = budget.limitAmount - spent
                val progress = if (budget.limitAmount > 0L) {
                    (spent.toFloat() / budget.limitAmount.toFloat()).coerceAtLeast(0f)
                } else {
                    0f
                }
                val status = when {
                    spent > budget.limitAmount -> BudgetHealthStatus.OVER_BUDGET
                    spent * 10L >= budget.limitAmount * 8L -> BudgetHealthStatus.NEAR_LIMIT
                    else -> BudgetHealthStatus.ON_TRACK
                }
                CategoryBudgetSummary(
                    category = budget.category,
                    limitAmount = budget.limitAmount,
                    spentAmount = spent,
                    remainingAmount = remaining,
                    progress = progress,
                    status = status
                )
            }.sortedBy { it.category.ordinal }
        }
    }
}
