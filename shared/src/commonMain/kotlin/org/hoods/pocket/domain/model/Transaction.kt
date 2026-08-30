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
    val date: Instant
) {
    init {
        require(description.isNotBlank()) { "Transaction description must not be blank" }
        require(amount > 0L) { "Transaction amount must be greater than zero, got $amount" }
    }
}
