package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BudgetDashboardScreen(
    initialMonth: YearMonth = YearMonth.now(),
    viewModel: BudgetDashboardViewModel = koinViewModel(key = "budget-$initialMonth") { parametersOf(initialMonth) },
    onSetBudgetClick: (YearMonth) -> Unit = {},
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BudgetDashboardContent(
        uiState = uiState,
        onSetBudgetClick = { onSetBudgetClick(uiState.selectedMonth) },
        onPreviousMonthClick = viewModel::onPreviousMonthClick,
        onNextMonthClick = viewModel::onNextMonthClick,
        onCurrentMonthClick = viewModel::onCurrentMonthClick,
        layoutMode = layoutMode,
        modifier = modifier
    )
}
