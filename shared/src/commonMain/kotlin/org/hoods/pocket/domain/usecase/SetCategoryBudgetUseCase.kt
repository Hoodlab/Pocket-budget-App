package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class SetCategoryBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository
) {
    suspend operator fun invoke(
        yearMonth: YearMonth,
        category: TransactionCategory,
        amountCents: Long
    ): Result<CategoryBudget> = runCatching {
        val monthlyBudget = budgetRepository.getMonthlyBudget(yearMonth).first()
            ?: throw IllegalStateException("A monthly budget must be set before allocating category budgets")

        if (category.applicableType != TransactionType.EXPENSE) {
            throw IllegalArgumentException("Category ${category.displayName} is not eligible for a category budget")
        }

        if (amountCents <= 0L) {
            throw IllegalArgumentException("Category budget limit must be greater than zero, got $amountCents")
        }

        val existingAllocations = categoryBudgetRepository.getCategoryBudgets(yearMonth).first()
        val otherAllocationsTotal = existingAllocations
            .filter { it.category != category }
            .sumOf { it.limitAmount }

        val newTotal = otherAllocationsTotal + amountCents
        if (newTotal > monthlyBudget.limitAmount) {
            throw IllegalArgumentException(
                "Category allocations cannot exceed the total monthly budget of ${monthlyBudget.limitAmount} cents. Attempted total: $newTotal cents."
            )
        }

        val budget = CategoryBudget(
            yearMonth = yearMonth,
            category = category,
            limitAmount = amountCents
        )
        categoryBudgetRepository.setCategoryBudget(budget)
        budget
    }
}
