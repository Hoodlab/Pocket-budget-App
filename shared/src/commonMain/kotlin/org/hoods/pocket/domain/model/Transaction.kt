package org.hoods.pocket.domain.model

import kotlin.time.Instant

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class Transaction(
    val id: String,
    val description: String,
    val amount: Long,
    val type: TransactionType,
    val date: Instant,
    val category: TransactionCategory = TransactionCategory.UNCATEGORIZED
) {
    init {
        require(description.isNotBlank()) { "Transaction description must not be blank" }
        require(amount > 0L) { "Transaction amount must be greater than zero, got $amount" }
        require(category.isValidFor(type)) { "Category ${category.displayName} is not valid for transaction type $type" }
    }
}
