package org.hoods.pocket.presentation.transaction.add

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
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.domain.util.MoneyParser

@OptIn(ExperimentalUuidApi::class)
class AddTransactionViewModel(
    private val addTransactionUseCase: AddTransactionUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddTransactionEvent>(Channel.BUFFERED)
    val events: Flow<AddTransactionEvent> = _events.receiveAsFlow()

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

    fun saveTransaction() {
        val currentState = _uiState.value
        if (currentState.isSaving) return // Prevent duplicate submissions

        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Transaction description must not be blank") }
            return
        }

        val cents = MoneyParser.parseAmountToCents(currentState.amountText)
        if (cents == null || cents <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount greater than zero") }
            return
        }

        val transaction = Transaction(
            id = Uuid.random().toString(),
            description = currentState.description.trim(),
            amount = cents,
            type = currentState.selectedType,
            date = Clock.System.now()
        )

        _uiState.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                addTransactionUseCase(transaction)
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                _events.send(AddTransactionEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save transaction"
                    )
                }
            }
        }
    }
}
