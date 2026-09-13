package org.hoods.pocket.domain.model

data class CategoryBudgetSummary(
    val category: TransactionCategory,
    val limitAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long,
    val progress: Float,
    val status: BudgetHealthStatus
) {
    init {
        require(category.applicableType == TransactionType.EXPENSE) {
            "CategoryBudgetSummary only applies to expense categories"
        }
        require(limitAmount > 0L) {
            "Category budget limit must be greater than zero"
        }
    }
}
