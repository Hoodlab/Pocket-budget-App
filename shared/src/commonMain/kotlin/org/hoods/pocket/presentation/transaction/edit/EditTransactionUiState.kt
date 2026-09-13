package org.hoods.pocket.presentation.transaction.edit

import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import kotlin.time.Instant

data class EditTransactionUiState(
    val initialTransaction: Transaction? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: TransactionCategory = TransactionCategory.UNCATEGORIZED,
    val description: String = "",
    val amountText: String = "",
    val selectedDate: Instant? = null,
    val dateString: String = "",
    val showDatePicker: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false
)
