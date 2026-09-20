package org.hoods.pocket.presentation.transaction.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = koinViewModel(),
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
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
        layoutMode = layoutMode,
        modifier = modifier
    )
}
