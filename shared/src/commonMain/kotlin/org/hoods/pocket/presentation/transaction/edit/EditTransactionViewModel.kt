package org.hoods.pocket.presentation.transaction.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.DeleteTransactionUseCase
import org.hoods.pocket.domain.usecase.GetTransactionByIdUseCase
import org.hoods.pocket.domain.usecase.UpdateTransactionUseCase
import org.hoods.pocket.domain.util.DateUtils
import org.hoods.pocket.domain.util.MoneyParser
import kotlin.time.Instant

class EditTransactionViewModel(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditTransactionEvent>(Channel.BUFFERED)
    val events: Flow<EditTransactionEvent> = _events.receiveAsFlow()

    fun loadTransaction(transactionId: String) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            val transaction = getTransactionByIdUseCase(transactionId)
            if (transaction != null) {
                _uiState.update {
                    it.copy(
                        initialTransaction = transaction,
                        selectedType = transaction.type,
                        description = transaction.description,
                        amountText = MoneyParser.formatCentsToAmount(transaction.amount),
                        selectedDate = transaction.date,
                        dateString = DateUtils.formatDate(transaction.date),
                        errorMessage = null,
                        isLoading = false,
                        isNotFound = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNotFound = true
                    )
                }
            }
        }
    }

    fun onTypeSelected(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type, errorMessage = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description, errorMessage = null) }
    }

    fun onAmountChanged(amountText: String) {
        if (amountText.all { it.isDigit() || it == '.' || it == '$' }) {
            val cleaned = amountText.removePrefix("$").trim()
            _uiState.update { it.copy(amountText = cleaned, errorMessage = null) }
        }
    }

    fun onOpenDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun onDateSelected(date: Instant) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                dateString = DateUtils.formatDate(date),
                showDatePicker = false
            )
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun saveChanges() {
        val currentState = _uiState.value
        val original = currentState.initialTransaction ?: return
        if (currentState.isSaving || currentState.isDeleting) return // Prevent duplicate submissions

        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Transaction description must not be blank") }
            return
        }

        val cents = MoneyParser.parseAmountToCents(currentState.amountText)
        if (cents == null || cents <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount greater than zero") }
            return
        }

        val updatedTransaction = original.copy(
            description = currentState.description.trim(),
            amount = cents,
            type = currentState.selectedType,
            date = currentState.selectedDate ?: original.date
        )

        _uiState.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                updateTransactionUseCase(updatedTransaction)
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                _events.send(EditTransactionEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save transaction changes"
                    )
                }
            }
        }
    }

    fun confirmDelete() {
        val original = _uiState.value.initialTransaction ?: return
        if (_uiState.value.isDeleting || _uiState.value.isSaving) return // Prevent duplicate deletion

        _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false) }
        scope.launch {
            try {
                deleteTransactionUseCase(original.id)
                _uiState.update { it.copy(isDeleting = false, errorMessage = null) }
                _events.send(EditTransactionEvent.DeleteSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = e.message ?: "Failed to delete transaction"
                    )
                }
            }
        }
    }
}
