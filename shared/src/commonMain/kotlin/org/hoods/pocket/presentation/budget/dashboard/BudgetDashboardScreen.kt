package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun BudgetDashboardScreen(
    viewModel: BudgetDashboardViewModel,
    onSetBudgetClick: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BudgetDashboardContent(
        uiState = uiState,
        onSetBudgetClick = onSetBudgetClick,
        onNavigateToTransactions = onNavigateToTransactions,
        onPreviousMonthClick = viewModel::onPreviousMonthClick,
        onNextMonthClick = viewModel::onNextMonthClick,
        onCurrentMonthClick = viewModel::onCurrentMonthClick,
        modifier = modifier
    )
}
