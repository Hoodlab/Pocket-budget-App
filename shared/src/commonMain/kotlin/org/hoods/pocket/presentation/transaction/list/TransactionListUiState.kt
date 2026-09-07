package org.hoods.pocket.presentation.transaction.list

import org.hoods.pocket.domain.model.Transaction

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
)
