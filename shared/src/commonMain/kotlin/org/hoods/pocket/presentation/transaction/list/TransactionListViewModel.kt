package org.hoods.pocket.presentation.transaction.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.GetTransactionsByMonthUseCase

class TransactionListViewModel(
    getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    initialMonth: YearMonth? = null,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _selectedMonth = MutableStateFlow(initialMonth ?: YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListUiState> = _selectedMonth.flatMapLatest { selectedMonth ->
        getTransactionsByMonthUseCase(selectedMonth).map { transactions ->
            val currentMonth = YearMonth.now()
            TransactionListUiState(
                 selectedMonth = selectedMonth,
                isCurrentMonth = selectedMonth == currentMonth,
                transactions = transactions,
                isLoading = false
            )
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TransactionListUiState(
            selectedMonth = initialMonth ?: YearMonth.now(),
            isCurrentMonth = (initialMonth ?: YearMonth.now()) == YearMonth.now(),
            isLoading = true
        )
    )

    fun onPreviousMonth() {
        _selectedMonth.update { it.previous() }
    }

    fun onNextMonth() {
        _selectedMonth.update { it.next() }
    }

    fun onCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }
}
