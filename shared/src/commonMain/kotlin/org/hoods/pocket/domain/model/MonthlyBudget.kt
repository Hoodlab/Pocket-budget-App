package org.hoods.pocket.domain.model

data class MonthlyBudget(
    val yearMonth: YearMonth,
    val limitAmount: Long
)
