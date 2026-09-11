package org.hoods.pocket.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object TransactionList : Screen

    @Serializable
    data object AddTransaction : Screen

    @Serializable
    data class EditTransaction(val transactionId: String) : Screen

    @Serializable
    data class BudgetDashboard(val year: Int? = null, val month: Int? = null) : Screen

    @Serializable
    data class SetBudget(val year: Int, val month: Int) : Screen
}
