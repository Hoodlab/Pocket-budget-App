package org.hoods.pocket.presentation.transaction.add

sealed interface AddTransactionEvent {
    data object SaveSuccess : AddTransactionEvent
}
