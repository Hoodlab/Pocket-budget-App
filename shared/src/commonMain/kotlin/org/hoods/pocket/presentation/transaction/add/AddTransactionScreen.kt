package org.hoods.pocket.presentation.transaction.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onSaveSuccess: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddTransactionEvent.SaveSuccess -> onSaveSuccess()
            }
        }
    }

    AddTransactionContent(
        uiState = uiState,
        onTypeSelected = viewModel::onTypeSelected,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onSaveClick = viewModel::saveTransaction,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
