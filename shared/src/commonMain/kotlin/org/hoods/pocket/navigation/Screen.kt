package org.hoods.pocket.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object TransactionList : Screen

    @Serializable
    data object AddTransaction : Screen

    @Serializable
    data class EditTransaction(val transactionId: String) : Screen
}
