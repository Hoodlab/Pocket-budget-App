package org.hoods.pocket.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hoods.pocket.domain.model.SyncStatus
import org.hoods.pocket.domain.usecase.GetSyncStatusUseCase

class PocketShellViewModel(
    private val getSyncStatusUseCase: GetSyncStatusUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.SignedOut)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        scope.launch {
            getSyncStatusUseCase().collect { status ->
                _syncStatus.value = status
            }
        }
    }
}
