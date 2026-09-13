package org.hoods.pocket.domain.model

data class CashFlowSummary(
    val yearMonth: YearMonth,
    val income: Long,
    val expense: Long,
    val netBalance: Long = income - expense
) {
    init {
        require(income >= 0L) { "Income must not be negative" }
        require(expense >= 0L) { "Expense must not be negative" }
    }
}
