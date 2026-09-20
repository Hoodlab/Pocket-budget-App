package org.hoods.pocket.presentation.auth.sign_in

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = koinViewModel(),
    onSignInSuccess: (userId: String) -> Unit,
    onClaimRequired: (userId: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SignInEvent.SignInSuccess -> onSignInSuccess(event.user.id)
                is SignInEvent.ClaimRequired -> onClaimRequired(event.userId)
                is SignInEvent.SignUpConfirmationSent -> {
                    // Stay on screen and show confirmation message
                }
            }
        }
    }

    SignInContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onToggleMode = viewModel::onToggleMode,
        onSubmit = viewModel::onSubmit,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
