package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class SaveCategoryAllocationsUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository
) {
    suspend operator fun invoke(
        yearMonth: YearMonth,
        totalLimitCents: Long,
        categoryAllocations: Map<TransactionCategory, Long?>
    ): Result<Unit> = runCatching {
        if (totalLimitCents <= 0L) {
            throw IllegalArgumentException("Monthly budget limit must be greater than zero")
        }

        categoryAllocations.keys.forEach { category ->
            if (category.applicableType != TransactionType.EXPENSE) {
                throw IllegalArgumentException("Category ${category.displayName} is not eligible for category budget")
            }
        }

        val positiveAllocations = categoryAllocations.mapNotNull { (category, amount) ->
            if (amount != null && amount > 0L) {
                CategoryBudget(yearMonth, category, amount)
            } else {
                null
            }
        }

        val totalAllocated = positiveAllocations.sumOf { it.limitAmount }
        if (totalAllocated > totalLimitCents) {
            throw IllegalArgumentException(
                "Total category allocations ($totalAllocated cents) cannot exceed monthly budget limit ($totalLimitCents cents)"
            )
        }

        budgetRepository.setMonthlyBudget(MonthlyBudget(yearMonth, totalLimitCents))

        categoryAllocations.forEach { (category, amount) ->
            if (amount == null || amount <= 0L) {
                categoryBudgetRepository.removeCategoryBudget(yearMonth, category)
            }
        }

        if (positiveAllocations.isNotEmpty()) {
            categoryBudgetRepository.setCategoryBudgets(positiveAllocations)
        }
    }
}
