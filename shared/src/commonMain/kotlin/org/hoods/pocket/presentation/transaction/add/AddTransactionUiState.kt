package org.hoods.pocket.presentation.transaction.add

import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import kotlin.time.Clock
import kotlin.time.Instant

data class AddTransactionUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: TransactionCategory = TransactionCategory.UNCATEGORIZED,
    val description: String = "",
    val amountText: String = "",
    val selectedDate: Instant = Clock.System.now(),
    val dateString: String = "Today",
    val showDatePicker: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)
