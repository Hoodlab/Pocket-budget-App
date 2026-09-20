package org.hoods.pocket

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.hoods.pocket.data.local.database.createBudgetRepository
import org.hoods.pocket.data.local.database.createCategoryBudgetRepository
import org.hoods.pocket.data.local.database.createDataOwnershipRepository
import org.hoods.pocket.data.local.database.createTransactionRepository
import org.hoods.pocket.data.local.database.getSharedAuthRepository

fun main() = application {
    val authRepository = getSharedAuthRepository()
    val repository = createTransactionRepository(authRepository)
    val budgetRepository = createBudgetRepository(authRepository)
    val categoryBudgetRepository = createCategoryBudgetRepository(authRepository)
    val dataOwnershipRepository = createDataOwnershipRepository()
    Window(
        onCloseRequest = ::exitApplication,
        title = "pocket-budget",
    ) {
        App(
            repository = repository,
            budgetRepository = budgetRepository,
            categoryBudgetRepository = categoryBudgetRepository,
            dataOwnershipRepository = dataOwnershipRepository,
            authRepository = authRepository
        )
    }
}