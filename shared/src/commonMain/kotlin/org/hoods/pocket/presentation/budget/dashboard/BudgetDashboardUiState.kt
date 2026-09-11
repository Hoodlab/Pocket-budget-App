package org.hoods.pocket.presentation.budget.dashboard

import org.hoods.pocket.domain.model.MonthlyBudgetSummary
import org.hoods.pocket.domain.model.YearMonth

data class BudgetDashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val isCurrentMonth: Boolean = true,
    val summary: MonthlyBudgetSummary? = null,
    val isLoading: Boolean = false
) {
    val hasBudget: Boolean get() = summary?.budget != null
    val monthTitle: String get() = selectedMonth.format()
}
