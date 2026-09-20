package org.hoods.pocket.presentation.auth.setup

import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.remote.SupabaseEnvironment

data class SetupConfigUiState(
    val currentEnvironment: SupabaseEnvironment = SupabaseEnvironment.LOCAL,
    val currentUrl: String = "",
    val isCustom: Boolean = false,
    val infoMessage: String? = null
)

sealed interface SetupConfigEvent {
    data class ConfigSelected(val config: SupabaseConfig) : SetupConfigEvent
    data object ContinueOffline : SetupConfigEvent
}
