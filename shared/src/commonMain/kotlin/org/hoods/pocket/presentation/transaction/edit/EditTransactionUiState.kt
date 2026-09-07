package org.hoods.pocket.presentation.transaction.edit

import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType

data class EditTransactionUiState(
    val initialTransaction: Transaction? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val description: String = "",
    val amountText: String = "",
    val dateString: String = "",
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false
)
