package org.hoods.pocket.presentation.auth.setup

import org.hoods.pocket.data.remote.SupabaseEnvironment

data class SetupConfigUiState(
    val environment: SupabaseEnvironment = SupabaseEnvironment.UNCONFIGURED,
    val url: String = "",
    val hasPublishableKey: Boolean = false,
    val isConfigured: Boolean = false
)

sealed interface SetupConfigEvent {
    data object NavigateBack : SetupConfigEvent
}
