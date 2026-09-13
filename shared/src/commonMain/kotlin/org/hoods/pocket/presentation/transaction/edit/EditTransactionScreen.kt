package org.hoods.pocket.presentation.transaction.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun EditTransactionScreen(
    viewModel: EditTransactionViewModel,
    transactionId: String,
    onSaveSuccess: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditTransactionEvent.SaveSuccess -> onSaveSuccess()
                EditTransactionEvent.DeleteSuccess -> onDeleteSuccess()
            }
        }
    }

    if (uiState.isNotFound) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }

    if (uiState.initialTransaction != null) {
        EditTransactionContent(
            uiState = uiState,
            onTypeSelected = viewModel::onTypeSelected,
            onCategorySelected = viewModel::onCategorySelected,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onAmountChanged = viewModel::onAmountChanged,
            onRequestDelete = viewModel::requestDelete,
            onDismissDelete = viewModel::dismissDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onSaveClick = viewModel::saveChanges,
            onDismiss = onDismiss,
            onOpenDatePicker = viewModel::onOpenDatePicker,
            onDismissDatePicker = viewModel::onDismissDatePicker,
            onDateSelected = viewModel::onDateSelected,
            modifier = modifier
        )
    }
}
