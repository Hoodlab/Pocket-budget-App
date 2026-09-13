package org.hoods.pocket.presentation.transaction.list

import org.hoods.pocket.domain.model.CashFlowSummary
import org.hoods.pocket.domain.model.CategoryBudgetSummary
import org.hoods.pocket.domain.model.MonthlyBudgetSummary
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.YearMonth

data class TransactionListUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val isCurrentMonth: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val cashFlow: CashFlowSummary? = null,
    val budgetSummary: MonthlyBudgetSummary? = null,
    val categorySummaries: List<CategoryBudgetSummary> = emptyList(),
    val isLoading: Boolean = false
)
