package org.hoods.pocket.presentation.budget.set

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
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.SetMonthlyBudgetUseCase
import org.hoods.pocket.domain.util.MoneyParser

class SetBudgetViewModel(
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    yearMonth: YearMonth,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        SetBudgetUiState(yearMonth = yearMonth)
    )
    val uiState: StateFlow<SetBudgetUiState> = _uiState.asStateFlow()

    private val _events = Channel<SetBudgetEvent>(Channel.BUFFERED)
    val events: Flow<SetBudgetEvent> = _events.receiveAsFlow()

    fun onAmountChanged(amountText: String) {
        if (amountText.all { it.isDigit() || it == '.' || it == '$' }) {
            val cleaned = amountText.removePrefix("$").trim()
            _uiState.update { it.copy(amountText = cleaned, errorMessage = null) }
        }
    }

    fun saveBudget() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        if (currentState.amountText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Budget amount must not be blank") }
            return
        }

        val cents = MoneyParser.parseAmountToCents(currentState.amountText)
        if (cents == null || cents <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount greater than zero") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                setMonthlyBudgetUseCase(
                    yearMonth = currentState.yearMonth,
                    amountCents = cents
                ).getOrThrow()
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                _events.send(SetBudgetEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save budget"
                    )
                }
            }
        }
    }
}
