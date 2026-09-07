package org.hoods.pocket.presentation.transaction.add

import org.hoods.pocket.domain.model.TransactionType

data class AddTransactionUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val description: String = "",
    val amountText: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)
