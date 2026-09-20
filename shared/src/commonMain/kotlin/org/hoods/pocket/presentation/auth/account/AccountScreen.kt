package org.hoods.pocket.presentation.auth.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onSignOutSuccess: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AccountEvent.SignOutSuccess -> onSignOutSuccess()
                is AccountEvent.NavigateToAuth -> onNavigateToAuth()
                is AccountEvent.NavigateToSetup -> onNavigateToSetup()
            }
        }
    }

    AccountContent(
        uiState = uiState,
        onSignOut = viewModel::onSignOut,
        onNavigateToAuth = viewModel::onNavigateToAuth,
        onNavigateToSetup = viewModel::onNavigateToSetup,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
