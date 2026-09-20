package org.hoods.pocket.presentation.transaction.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel = koinViewModel(),
    onSaveSuccess: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
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
        onCategorySelected = viewModel::onCategorySelected,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onSaveClick = viewModel::saveTransaction,
        onDismiss = onDismiss,
        onOpenDatePicker = viewModel::onOpenDatePicker,
        onDismissDatePicker = viewModel::onDismissDatePicker,
        onDateSelected = viewModel::onDateSelected,
        layoutMode = layoutMode,
        modifier = modifier
    )
}
