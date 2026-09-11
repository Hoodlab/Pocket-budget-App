package org.hoods.pocket.domain.model

enum class BudgetHealthStatus {
    ON_TRACK,
    NEAR_LIMIT,
    OVER_BUDGET
}

data class MonthlyBudgetSummary(
    val yearMonth: YearMonth,
    val budget: MonthlyBudget?,
    val totalExpense: Long,
    val remainingAmount: Long?,
    val progress: Float,
    val status: BudgetHealthStatus?
)
