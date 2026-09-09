package org.hoods.pocket.presentation.transaction.list

import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.YearMonth

data class TransactionListUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val isCurrentMonth: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
)
