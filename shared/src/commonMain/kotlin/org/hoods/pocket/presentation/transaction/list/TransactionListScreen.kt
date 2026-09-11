package org.hoods.pocket.presentation.transaction.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    TransactionListContent(
        uiState = uiState,
        onAddTransactionClick = onAddTransactionClick,
        onTransactionClick = onTransactionClick,
        onPreviousMonthClick = viewModel::onPreviousMonth,
        onNextMonthClick = viewModel::onNextMonth,
        onCurrentMonthClick = viewModel::onCurrentMonth,
        onNavigateToBudgets = onNavigateToBudgets,
        modifier = modifier
    )
}
