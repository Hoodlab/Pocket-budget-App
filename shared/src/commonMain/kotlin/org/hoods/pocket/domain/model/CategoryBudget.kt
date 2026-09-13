package org.hoods.pocket.domain.model

data class CategoryBudget(
    val yearMonth: YearMonth,
    val category: TransactionCategory,
    val limitAmount: Long
) {
    init {
        require(category.applicableType == TransactionType.EXPENSE) {
            "Category ${category.displayName} is not an eligible expense category for budget allocation"
        }
        require(limitAmount > 0L) {
            "Category budget limit amount must be greater than zero, got $limitAmount"
        }
    }
}
