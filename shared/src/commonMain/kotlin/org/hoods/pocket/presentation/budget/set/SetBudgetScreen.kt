package org.hoods.pocket.presentation.budget.set

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun SetBudgetScreen(
    viewModel: SetBudgetViewModel,
    onSaveSuccess: () -> Unit,
    onDeleteSuccess: () -> Unit = {},
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetBudgetEvent.SaveSuccess -> onSaveSuccess()
                SetBudgetEvent.DeleteSuccess -> onDeleteSuccess()
            }
        }
    }

    SetBudgetContent(
        uiState = uiState,
        onAmountChanged = viewModel::onAmountChanged,
        onCategoryAllocationChanged = viewModel::onCategoryAllocationChanged,
        onRemoveCategoryAllocation = viewModel::onRemoveCategoryAllocation,
        onResetAllocations = viewModel::onResetAllocations,
        onSaveClick = viewModel::saveBudget,
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
