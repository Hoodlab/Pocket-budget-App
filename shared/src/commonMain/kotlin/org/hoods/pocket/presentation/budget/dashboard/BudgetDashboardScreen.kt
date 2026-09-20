package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode

@Composable
fun BudgetDashboardScreen(
    viewModel: BudgetDashboardViewModel,
    onSetBudgetClick: () -> Unit,
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BudgetDashboardContent(
        uiState = uiState,
        onSetBudgetClick = onSetBudgetClick,
        onPreviousMonthClick = viewModel::onPreviousMonthClick,
        onNextMonthClick = viewModel::onNextMonthClick,
        onCurrentMonthClick = viewModel::onCurrentMonthClick,
        layoutMode = layoutMode,
        modifier = modifier
    )
}
