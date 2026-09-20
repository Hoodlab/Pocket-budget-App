package org.hoods.pocket.presentation.auth.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import org.hoods.pocket.data.remote.SupabaseConfig

@Composable
fun SetupConfigScreen(
    viewModel: SetupConfigViewModel,
    onConfigChanged: (SupabaseConfig) -> Unit,
    onContinueOffline: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SetupConfigEvent.ConfigSelected -> {
                    onConfigChanged(event.config)
                }
                is SetupConfigEvent.ContinueOffline -> {
                    onContinueOffline()
                }
            }
        }
    }

    SetupConfigContent(
        uiState = uiState,
        onSelectLocal = viewModel::selectLocal,
        onSelectHostedDev = viewModel::selectHostedDev,
        onSelectOffline = viewModel::selectOffline,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
