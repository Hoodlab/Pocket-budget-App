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

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase
import org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase

class TransactionListViewModel(
    getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    getCashFlowSummaryUseCase: GetCashFlowSummaryUseCase? = null,
    getMonthlyBudgetSummaryUseCase: GetMonthlyBudgetSummaryUseCase? = null,
    getCategoryBudgetSummariesUseCase: GetCategoryBudgetSummariesUseCase? = null,
    initialMonth: YearMonth? = null,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _selectedMonth = MutableStateFlow(initialMonth ?: YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListUiState> = _selectedMonth.flatMapLatest { selectedMonth ->
        val txFlow = getTransactionsByMonthUseCase(selectedMonth)
        val cashFlowFlow = getCashFlowSummaryUseCase?.invoke(selectedMonth) ?: flowOf(null)
        val budgetFlow = getMonthlyBudgetSummaryUseCase?.invoke(selectedMonth) ?: flowOf(null)
        val categoryFlow = getCategoryBudgetSummariesUseCase?.invoke(selectedMonth) ?: flowOf(emptyList())

        combine(txFlow, cashFlowFlow, budgetFlow, categoryFlow) { transactions, cashFlow, budgetSummary, categorySummaries ->
            val currentMonth = YearMonth.now()
            TransactionListUiState(
                selectedMonth = selectedMonth,
                isCurrentMonth = selectedMonth == currentMonth,
                transactions = transactions,
                cashFlow = cashFlow,
                budgetSummary = budgetSummary,
                categorySummaries = categorySummaries,
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
