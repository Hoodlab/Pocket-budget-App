package org.hoods.pocket.presentation.budget.set

import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.util.MoneyParser

data class SetBudgetUiState(
    val yearMonth: YearMonth,
    val amountText: String = "",
    val categoryAllocations: Map<TransactionCategory, String> = emptyMap(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isExistingBudget: Boolean = false,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false
) {
    val monthTitle: String get() = yearMonth.format()
    val hasExistingBudget: Boolean get() = isExistingBudget

    val totalBudgetCents: Long?
        get() = MoneyParser.parseAmountToCents(amountText)

    val totalAllocatedCents: Long
        get() = categoryAllocations.values.sumOf { text ->
            MoneyParser.parseAmountToCents(text) ?: 0L
        }

    val remainingCents: Long?
        get() = totalBudgetCents?.let { total ->
            total - totalAllocatedCents
        }

    val isOverAllocated: Boolean
        get() = totalBudgetCents?.let { total ->
            totalAllocatedCents > total
        } ?: false

    val overAllocatedCents: Long
        get() = totalBudgetCents?.let { total ->
            (totalAllocatedCents - total).coerceAtLeast(0L)
        } ?: 0L
}
