package org.hoods.pocket

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.hoods.pocket.data.local.database.createBudgetRepository
import org.hoods.pocket.data.local.database.createCategoryBudgetRepository
import org.hoods.pocket.data.local.database.createTransactionRepository

fun main() = application {
    val repository = createTransactionRepository()
    val budgetRepository = createBudgetRepository()
    val categoryBudgetRepository = createCategoryBudgetRepository()
    Window(
        onCloseRequest = ::exitApplication,
        title = "pocket-budget",
    ) {
        App(
            repository = repository,
            budgetRepository = budgetRepository,
            categoryBudgetRepository = categoryBudgetRepository
        )
    }
}