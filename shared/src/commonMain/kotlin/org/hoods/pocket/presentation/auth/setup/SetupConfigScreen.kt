package org.hoods.pocket.presentation.auth.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SetupConfigScreen(
    viewModel: SetupConfigViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SetupConfigEvent.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    SetupConfigContent(
        uiState = uiState,
        onBackClick = viewModel::onDoneClick,
        modifier = modifier
    )
}
