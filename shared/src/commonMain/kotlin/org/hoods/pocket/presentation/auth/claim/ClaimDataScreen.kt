package org.hoods.pocket.presentation.auth.claim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ClaimDataScreen(
    viewModel: ClaimDataViewModel,
    onClaimFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ClaimDataEvent.ClaimCompleted -> onClaimFinished()
                is ClaimDataEvent.KeepOnDeviceCompleted -> onClaimFinished()
            }
        }
    }

    ClaimDataContent(
        uiState = uiState,
        onClaimForAccount = viewModel::onClaimForAccount,
        onKeepOnDevice = viewModel::onKeepOnDevice,
        modifier = modifier
    )
}
