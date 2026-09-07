package org.hoods.pocket.presentation.transaction.edit

sealed interface EditTransactionEvent {
    data object SaveSuccess : EditTransactionEvent
    data object DeleteSuccess : EditTransactionEvent
}
