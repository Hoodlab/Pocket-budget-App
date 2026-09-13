package org.hoods.pocket.presentation.budget.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDashboardViewModel(
    getMonthlyBudgetSummaryUseCase: GetMonthlyBudgetSummaryUseCase,
    getCategoryBudgetSummariesUseCase: GetCategoryBudgetSummariesUseCase? = null,
    initialMonth: YearMonth = YearMonth.now(),
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _selectedMonth = MutableStateFlow(initialMonth)

    val uiState: StateFlow<BudgetDashboardUiState> = _selectedMonth.flatMapLatest { selectedMonth ->
        val monthlyFlow = getMonthlyBudgetSummaryUseCase(selectedMonth)
        val categoryFlow = getCategoryBudgetSummariesUseCase?.invoke(selectedMonth) ?: flowOf(emptyList())

        combine(monthlyFlow, categoryFlow) { summary, categorySummaries ->
            val currentMonth = YearMonth.now()
            BudgetDashboardUiState(
                selectedMonth = summary.yearMonth,
                isCurrentMonth = summary.yearMonth == currentMonth,
                summary = summary,
                categorySummaries = categorySummaries,
                isLoading = false
            )
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BudgetDashboardUiState(
            selectedMonth = initialMonth,
            isCurrentMonth = initialMonth == YearMonth.now(),
            isLoading = true
        )
    )

    private val _events = MutableSharedFlow<BudgetDashboardEvent>()
    val events: SharedFlow<BudgetDashboardEvent> = _events.asSharedFlow()

    fun onPreviousMonthClick() {
        _selectedMonth.update { it.previous() }
    }

    fun onNextMonthClick() {
        _selectedMonth.update { it.next() }
    }

    fun onCurrentMonthClick() {
        _selectedMonth.value = YearMonth.now()
    }
}
