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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    TransactionListContent(
        uiState = uiState,
        onAddTransactionClick = onAddTransactionClick,
        onTransactionClick = onTransactionClick,
        modifier = modifier
    )
}
