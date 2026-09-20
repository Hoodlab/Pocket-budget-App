package org.hoods.pocket.domain.model

data class UnclaimedDataSummary(
    val transactionCount: Int,
    val monthlyBudgetCount: Int,
    val categoryBudgetCount: Int
) {
    val hasUnclaimedData: Boolean
        get() = transactionCount > 0 || monthlyBudgetCount > 0 || categoryBudgetCount > 0

    val totalCount: Int
        get() = transactionCount + monthlyBudgetCount + categoryBudgetCount
}
