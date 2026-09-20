package org.hoods.pocket.presentation.auth.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.remote.SupabaseEnvironment

class SetupConfigViewModel(
    initialConfig: SupabaseConfig = SupabaseConfig.local(),
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        SetupConfigUiState(
            currentEnvironment = initialConfig.environment,
            currentUrl = initialConfig.url.orEmpty()
        )
    )
    val uiState: StateFlow<SetupConfigUiState> = _uiState.asStateFlow()

    private val _events = Channel<SetupConfigEvent>(Channel.BUFFERED)
    val events: Flow<SetupConfigEvent> = _events.receiveAsFlow()

    fun selectLocal() {
        val config = SupabaseConfig.local()
        _uiState.update {
            it.copy(
                currentEnvironment = SupabaseEnvironment.LOCAL,
                currentUrl = config.url.orEmpty(),
                infoMessage = "Connected to Local Supabase"
            )
        }
        scope.launch {
            _events.send(SetupConfigEvent.ConfigSelected(config))
        }
    }

    fun selectHostedDev() {
        val config = SupabaseConfig.hostedDev()
        _uiState.update {
            it.copy(
                currentEnvironment = SupabaseEnvironment.HOSTED_DEV,
                currentUrl = config.url.orEmpty(),
                infoMessage = "Connected to Hosted Dev Supabase"
            )
        }
        scope.launch {
            _events.send(SetupConfigEvent.ConfigSelected(config))
        }
    }

    fun selectOffline() {
        _uiState.update {
            it.copy(
                currentEnvironment = SupabaseEnvironment.UNCONFIGURED,
                currentUrl = "",
                infoMessage = "Running in Local Offline Mode"
            )
        }
        scope.launch {
            _events.send(SetupConfigEvent.ContinueOffline)
        }
    }
}
