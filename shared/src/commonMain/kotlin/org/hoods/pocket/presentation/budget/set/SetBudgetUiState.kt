package org.hoods.pocket.presentation.budget.set

import org.hoods.pocket.domain.model.YearMonth

data class SetBudgetUiState(
    val yearMonth: YearMonth,
    val amountText: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isExistingBudget: Boolean = false,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false
) {
    val monthTitle: String get() = yearMonth.format()
    val hasExistingBudget: Boolean get() = isExistingBudget
}
