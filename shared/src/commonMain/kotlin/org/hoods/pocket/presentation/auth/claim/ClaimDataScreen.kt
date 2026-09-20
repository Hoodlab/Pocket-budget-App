package org.hoods.pocket.presentation.auth.claim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ClaimDataScreen(
    targetUserId: String = "",
    viewModel: ClaimDataViewModel = koinViewModel(key = "claim-$targetUserId") { parametersOf(targetUserId) },
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
