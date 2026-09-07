package org.hoods.pocket.presentation.transaction.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.hoods.pocket.domain.usecase.GetTransactionsUseCase

class TransactionListViewModel(
    getTransactionsUseCase: GetTransactionsUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    val uiState: StateFlow<TransactionListUiState> = getTransactionsUseCase()
        .map { transactions ->
            TransactionListUiState(
                transactions = transactions,
                isLoading = false
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TransactionListUiState(isLoading = true)
        )
}
