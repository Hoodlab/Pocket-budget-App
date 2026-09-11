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
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetBudgetEvent.SaveSuccess -> onSaveSuccess()
            }
        }
    }

    SetBudgetContent(
        uiState = uiState,
        onAmountChanged = viewModel::onAmountChanged,
        onSaveClick = viewModel::saveBudget,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
