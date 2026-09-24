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
import kotlinx.coroutines.launch
import org.hoods.pocket.data.remote.SupabaseConfig

class SetupConfigViewModel(
    config: SupabaseConfig,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        SetupConfigUiState(
            environment = config.environment,
            url = config.url.orEmpty(),
            hasPublishableKey = !config.publishableKey.isNullOrBlank(),
            isConfigured = config.isConfigured
        )
    )
    val uiState: StateFlow<SetupConfigUiState> = _uiState.asStateFlow()

    private val _events = Channel<SetupConfigEvent>(Channel.BUFFERED)
    val events: Flow<SetupConfigEvent> = _events.receiveAsFlow()

    fun onDoneClick() {
        scope.launch {
            _events.send(SetupConfigEvent.NavigateBack)
        }
    }
}
